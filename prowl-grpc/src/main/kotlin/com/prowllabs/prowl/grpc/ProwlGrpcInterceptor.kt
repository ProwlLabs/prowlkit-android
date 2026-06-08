package com.prowllabs.prowl.grpc

import com.prowllabs.prowl.core.logging.ProwlEndpointRateAlerts
import com.prowllabs.prowl.core.model.NetworkLog
import com.prowllabs.prowl.core.model.NetworkProtocol
import com.prowllabs.prowl.core.runtime.ProwlRuntime
import io.grpc.CallOptions
import io.grpc.Channel
import io.grpc.ClientCall
import io.grpc.ClientInterceptor
import io.grpc.ForwardingClientCall
import io.grpc.ForwardingClientCallListener
import io.grpc.Metadata
import io.grpc.MethodDescriptor
import io.grpc.Status
import java.util.UUID

class ProwlGrpcInterceptor : ClientInterceptor {
    override fun <ReqT, RespT> interceptCall(
        method: MethodDescriptor<ReqT, RespT>,
        callOptions: CallOptions,
        next: Channel,
    ): ClientCall<ReqT, RespT> {
        if (!ProwlRuntime.isLoggingEnabled) {
            return next.newCall(method, callOptions)
        }

        val delegate = next.newCall(method, callOptions)
        return object : ForwardingClientCall.SimpleForwardingClientCall<ReqT, RespT>(delegate) {
            private val startedAt = System.currentTimeMillis()
            private val requestId = UUID.randomUUID()
            private var requestMessage: String = ""
            private var responseMessage: String = ""
            private val requestHeaders = linkedMapOf<String, String>()
            private val responseHeaders = linkedMapOf<String, String>()

            override fun start(responseListener: Listener<RespT>, headers: Metadata) {
                headers.keys().forEach { key ->
                    requestHeaders[key] = headers.get(Metadata.Key.of(key, Metadata.ASCII_STRING_MARSHALLER)).orEmpty()
                }
                super.start(
                    object : ForwardingClientCallListener.SimpleForwardingClientCallListener<RespT>(responseListener) {
                        override fun onHeaders(headers: Metadata) {
                            headers.keys().forEach { key ->
                                responseHeaders[key] = headers.get(Metadata.Key.of(key, Metadata.ASCII_STRING_MARSHALLER)).orEmpty()
                            }
                            super.onHeaders(headers)
                        }

                        override fun onMessage(message: RespT) {
                            responseMessage = message?.toString().orEmpty()
                            super.onMessage(message)
                        }

                        override fun onClose(status: Status, trailers: Metadata) {
                            trailers.keys().forEach { key ->
                                responseHeaders[key] = trailers.get(Metadata.Key.of(key, Metadata.ASCII_STRING_MARSHALLER)).orEmpty()
                            }
                            val duration = System.currentTimeMillis() - startedAt
                            val url = "grpc://${method.fullMethodName}"
                            val log = NetworkLog(
                                requestId = requestId,
                                url = url,
                                method = method.type.name,
                                requestHeaders = requestHeaders,
                                requestBody = NetworkLog.Body(
                                    requestMessage.toByteArray(Charsets.UTF_8),
                                    "application/grpc",
                                ),
                                responseHeaders = responseHeaders,
                                responseBody = NetworkLog.Body(
                                    responseMessage.toByteArray(Charsets.UTF_8),
                                    "application/grpc",
                                ),
                                statusCode = grpcStatusToHttp(status.code),
                                startedAtMillis = startedAt,
                                durationMillis = duration,
                                errorDescription = if (status.isOk) null else status.description,
                                protocol = NetworkProtocol.GRPC,
                            )
                            val finalLog = log.copy(
                                endpointRateAlertTriggered = ProwlEndpointRateAlerts.evaluate(log),
                            )
                            ProwlRuntime.storage.appendBlocking(finalLog)
                            ProwlRuntime.onLogsChanged()
                            super.onClose(status, trailers)
                        }
                    },
                    headers,
                )
            }

            override fun sendMessage(message: ReqT) {
                requestMessage = message?.toString().orEmpty()
                super.sendMessage(message)
            }
        }
    }

    private fun grpcStatusToHttp(code: Status.Code): Int = when (code) {
        Status.Code.OK -> 200
        Status.Code.INVALID_ARGUMENT -> 400
        Status.Code.UNAUTHENTICATED -> 401
        Status.Code.PERMISSION_DENIED -> 403
        Status.Code.NOT_FOUND -> 404
        Status.Code.ALREADY_EXISTS -> 409
        Status.Code.RESOURCE_EXHAUSTED -> 429
        Status.Code.UNIMPLEMENTED -> 501
        Status.Code.UNAVAILABLE -> 503
        Status.Code.DEADLINE_EXCEEDED -> 504
        else -> 500
    }
}
