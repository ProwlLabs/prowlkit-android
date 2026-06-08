package com.prowllabs.prowl.core.interceptor

import com.prowllabs.prowl.core.logging.ProwlEndpointRateAlerts
import com.prowllabs.prowl.core.model.NetworkLog
import com.prowllabs.prowl.core.runtime.ProwlRuntime
import com.prowllabs.prowl.core.util.BodyDecoder
import com.prowllabs.prowl.core.util.MultipartParser
import okhttp3.Headers
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import java.io.IOException
import java.util.UUID

class ProwlInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        if (!ProwlRuntime.isLoggingEnabled) {
            return chain.proceed(chain.request())
        }
        return runCatching { interceptCaptured(chain) }
            .getOrElse { error ->
                // Never crash the host app because of the debugger.
                runCatching { chain.proceed(chain.request()) }
                    .getOrElse { throw error }
            }
    }

    private fun interceptCaptured(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val url = originalRequest.url.toString()
        if (!shouldCapture(originalRequest)) {
            return chain.proceed(originalRequest)
        }

        val rewriteRule = ProwlRuntime.requestRewriter.findMatch(url, originalRequest.method)
        val rewrittenRequest = if (rewriteRule != null) {
            ProwlRuntime.requestRewriter.apply(originalRequest, rewriteRule)
        } else {
            originalRequest
        }
        val requestWasRewritten = rewriteRule != null

        val (request, requestBodyBytes) = captureRequestBody(rewrittenRequest)

        val callKey = ProwlTimingStore.keyFor(chain.call())
        val requestId = UUID.randomUUID()
        val startedAt = System.currentTimeMillis()
        val requestHeaders = request.headers.toMap()
        val requestContentType = request.body?.contentType()?.toString()
            ?: request.header("Content-Type")

        val effectiveUrl = request.url.toString()
        val mockRule = ProwlRuntime.mocker.findMatch(effectiveUrl, request.method)

        val response: Response
        val networkError: String?
        try {
            response = if (mockRule != null) {
                buildMockResponse(request, mockRule)
            } else {
                chain.proceed(request)
            }
            networkError = null
        } catch (error: IOException) {
            logFailedRequest(
                requestId = requestId,
                request = request,
                effectiveUrl = effectiveUrl,
                requestWasRewritten = requestWasRewritten,
                requestHeaders = requestHeaders,
                requestContentType = requestContentType,
                requestBodyBytes = requestBodyBytes,
                startedAt = startedAt,
                callKey = callKey,
                error = error,
            )
            throw error
        }

        val duration = System.currentTimeMillis() - startedAt
        val responseBodyBytes = peekResponseBody(response)
        val responseHeaders = response.headers.toMap()
        val responseEncoding = response.header("Content-Encoding")
        val responseContentType = response.header("Content-Type")
            ?: response.body?.contentType()?.toString()

        val requestEncoding = request.header("Content-Encoding")
        val decodedRequestBytes = requestBodyBytes?.let {
            BodyDecoder.decodeIfNeeded(it, requestEncoding)
        }
        val decodedResponseBytes = responseBodyBytes?.let {
            BodyDecoder.decodeIfNeeded(it, responseEncoding)
        }

        val requestBody = decodedRequestBytes?.let {
            NetworkLog.Body(it, requestContentType)
        }
        var responseBodyData = decodedResponseBytes ?: ByteArray(0)
        ProwlRuntime.responseBodyLoggingTransformer?.transform(responseBodyData, responseContentType)
            ?.let { responseBodyData = it }

        val maskedRequestHeaders = maybeMaskHeaders(requestHeaders)
        val maskedResponseHeaders = maybeMaskHeaders(responseHeaders)
        val maskedRequestBody = maybeMaskBody(requestBody)
        val maskedResponseBody = maybeMaskBody(
            NetworkLog.Body(responseBodyData, responseContentType),
        )

        val requestParts = maskedRequestBody?.let {
            MultipartParser.parse(it.data, it.contentType)
        }.orEmpty()
        val responseParts = maskedResponseBody?.let {
            MultipartParser.parse(it.data, it.contentType)
        }.orEmpty()

        val timing = ProwlTimingStore.take(callKey)
        val hostIp = ProwlTimingStore.takeHostIp(callKey)

        val provisionalLog = NetworkLog(
            requestId = requestId,
            url = effectiveUrl,
            method = request.method,
            requestHeaders = maskedRequestHeaders,
            requestBody = maskedRequestBody,
            responseHeaders = maskedResponseHeaders,
            responseBody = maskedResponseBody,
            statusCode = response.code,
            startedAtMillis = startedAt,
            durationMillis = duration,
            timeoutMillis = chain.readTimeoutMillis().toLong().takeIf { it >= 0 },
            errorDescription = networkError ?: if (response.isSuccessful) null else response.message,
            hostIp = hostIp,
            timing = timing,
            requestMultipartParts = requestParts,
            responseMultipartParts = responseParts,
            requestRewritten = requestWasRewritten,
        )

        val finalLog = provisionalLog.copy(
            endpointRateAlertTriggered = ProwlEndpointRateAlerts.evaluate(provisionalLog),
        )

        ProwlRuntime.storage.appendBlocking(finalLog)
        ProwlRuntime.onLogsChanged()
        return response
    }

    private fun logFailedRequest(
        requestId: UUID,
        request: Request,
        effectiveUrl: String,
        requestWasRewritten: Boolean,
        requestHeaders: Map<String, String>,
        requestContentType: String?,
        requestBodyBytes: ByteArray?,
        startedAt: Long,
        callKey: String,
        error: IOException,
    ) {
        val duration = System.currentTimeMillis() - startedAt
        val requestEncoding = request.header("Content-Encoding")
        val decodedRequestBytes = requestBodyBytes?.let {
            BodyDecoder.decodeIfNeeded(it, requestEncoding)
        }
        val requestBody = decodedRequestBytes?.let {
            NetworkLog.Body(it, requestContentType)
        }
        val maskedRequestHeaders = maybeMaskHeaders(requestHeaders)
        val maskedRequestBody = maybeMaskBody(requestBody)
        val requestParts = maskedRequestBody?.let {
            MultipartParser.parse(it.data, it.contentType)
        }.orEmpty()
        val timing = ProwlTimingStore.take(callKey)
        val hostIp = ProwlTimingStore.takeHostIp(callKey)

        val provisionalLog = NetworkLog(
            requestId = requestId,
            url = effectiveUrl,
            method = request.method,
            requestHeaders = maskedRequestHeaders,
            requestBody = maskedRequestBody,
            responseHeaders = emptyMap(),
            responseBody = null,
            statusCode = null,
            startedAtMillis = startedAt,
            durationMillis = duration,
            timeoutMillis = null,
            errorDescription = error.message ?: error.javaClass.simpleName,
            hostIp = hostIp,
            timing = timing,
            requestMultipartParts = requestParts,
            responseMultipartParts = emptyList(),
            requestRewritten = requestWasRewritten,
        )
        val finalLog = provisionalLog.copy(
            endpointRateAlertTriggered = ProwlEndpointRateAlerts.evaluate(provisionalLog),
        )
        ProwlRuntime.storage.appendBlocking(finalLog)
        ProwlRuntime.onLogsChanged()
    }

    private fun shouldCapture(request: Request): Boolean {
        val scheme = request.url.scheme.lowercase()
        if (scheme !in setOf("http", "https")) return false
        return !ProwlRuntime.shouldIgnore(request.url.toString())
    }

    private fun captureRequestBody(request: Request): Pair<Request, ByteArray?> {
        val body = request.body ?: return request to null
        if (body.isDuplex() || body.isOneShot()) return request to null
        val contentLength = body.contentLength()
        if (contentLength > MAX_CAPTURE_BYTES) return request to null
        return runCatching {
            Buffer().use { buffer ->
                body.writeTo(buffer)
                val bytes = buffer.readByteArray()
                val replayBody = bytes.toRequestBody(body.contentType())
                val replayRequest = request.newBuilder().method(request.method, replayBody).build()
                val capturedBytes = bytes.takeIf { it.size <= MAX_CAPTURE_BYTES }
                replayRequest to capturedBytes
            }
        }.getOrElse { request to null }
    }

    private fun peekResponseBody(response: Response): ByteArray? =
        runCatching { response.peekBody(MAX_PEEK_BYTES).bytes() }.getOrNull()

    private fun buildMockResponse(request: Request, rule: com.prowllabs.prowl.core.mocking.ProwlMockRule): Response {
        val statusCode = rule.mockStatusCode.coerceIn(100, 599)
        val mediaType = rule.mockHeaders["Content-Type"]?.toMediaTypeOrNull()
            ?: "application/json; charset=utf-8".toMediaTypeOrNull()
        val bodyBytes = rule.mockBody
        val body = bodyBytes.toResponseBody(mediaType)
        val headersBuilder = Headers.Builder()
        rule.mockHeaders.forEach { (key, value) ->
            if (!key.equals("Content-Length", ignoreCase = true) &&
                !key.equals("Content-Encoding", ignoreCase = true)
            ) {
                headersBuilder.add(key, value)
            }
        }
        headersBuilder.add("Content-Length", bodyBytes.size.toString())

        return Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(statusCode)
            .message(mockStatusMessage(statusCode))
            .headers(headersBuilder.build())
            .body(body)
            .sentRequestAtMillis(System.currentTimeMillis())
            .receivedResponseAtMillis(System.currentTimeMillis())
            .build()
    }

    private fun mockStatusMessage(statusCode: Int): String = when (statusCode) {
        200 -> "OK"
        201 -> "Created"
        204 -> "No Content"
        400 -> "Bad Request"
        401 -> "Unauthorized"
        403 -> "Forbidden"
        404 -> "Not Found"
        500 -> "Internal Server Error"
        else -> "Mocked by Prowl"
    }

    companion object {
        private const val MAX_PEEK_BYTES = 2L * 1024L * 1024L
        private const val MAX_CAPTURE_BYTES = 2L * 1024L * 1024L
    }

    private fun maybeMaskHeaders(headers: Map<String, String>): Map<String, String> =
        if (ProwlRuntime.isSensitiveDataMaskingEnabled) {
            ProwlRuntime.masker.mask(headers)
        } else {
            headers
        }

    private fun maybeMaskBody(body: NetworkLog.Body?): NetworkLog.Body? {
        if (body == null) return null
        return if (ProwlRuntime.isSensitiveDataMaskingEnabled) {
            ProwlRuntime.masker.mask(body.data, body.contentType)
        } else {
            body
        }
    }

    private fun Headers.toMap(): Map<String, String> {
        val result = linkedMapOf<String, String>()
        for (index in 0 until size) {
            val name = name(index)
            val existing = result[name]
            val value = value(index)
            result[name] = if (existing == null) value else "$existing, $value"
        }
        return result
    }
}
