package com.prowllabs.prowl.core.interceptor

import com.prowllabs.prowl.core.logging.ProwlEndpointRateAlerts
import com.prowllabs.prowl.core.model.NetworkLog
import com.prowllabs.prowl.core.runtime.ProwlRuntime
import okhttp3.Headers
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import java.util.UUID

/**
 * OkHttp interceptor that logs traffic, applies mock rules, and stores entries
 * in [ProwlRuntime.storage]. Register via [ProwlOkHttp.interceptor].
 */
class ProwlInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        if (!ProwlRuntime.isLoggingEnabled) {
            return chain.proceed(chain.request())
        }

        val request = chain.request()
        val url = request.url.toString()
        if (!shouldCapture(request)) {
            return chain.proceed(request)
        }

        val requestId = UUID.randomUUID()
        val startedAt = System.currentTimeMillis()
        val requestBodyBytes = readRequestBody(request)
        val requestHeaders = request.headers.toMap()

        val mockRule = ProwlRuntime.mocker.findMatch(url, request.method)
        val response = if (mockRule != null) {
            buildMockResponse(request, mockRule)
        } else {
            chain.proceed(request)
        }

        val duration = System.currentTimeMillis() - startedAt
        val responseBodyBytes = peekResponseBody(response)
        val responseHeaders = response.headers.toMap()

        val requestBody = requestBodyBytes?.let {
            NetworkLog.Body(it, request.body?.contentType()?.toString())
        }
        val responseContentType = response.header("Content-Type")
            ?: response.body?.contentType()?.toString()

        var responseBodyData = responseBodyBytes ?: ByteArray(0)
        ProwlRuntime.responseBodyLoggingTransformer?.transform(responseBodyData, responseContentType)
            ?.let { responseBodyData = it }

        val maskedRequestHeaders = maybeMaskHeaders(requestHeaders)
        val maskedResponseHeaders = maybeMaskHeaders(responseHeaders)
        val maskedRequestBody = maybeMaskBody(requestBody)
        val maskedResponseBody = maybeMaskBody(
            NetworkLog.Body(responseBodyData, responseContentType),
        )

        val provisionalLog = NetworkLog(
            requestId = requestId,
            url = url,
            method = request.method,
            requestHeaders = maskedRequestHeaders,
            requestBody = maskedRequestBody,
            responseHeaders = maskedResponseHeaders,
            responseBody = maskedResponseBody,
            statusCode = response.code,
            startedAtMillis = startedAt,
            durationMillis = duration,
            timeoutMillis = chain.readTimeoutMillis().toLong().takeIf { it >= 0 },
            errorDescription = if (response.isSuccessful) null else response.message,
        )

        val finalLog = provisionalLog.copy(
            endpointRateAlertTriggered = ProwlEndpointRateAlerts.evaluate(provisionalLog),
        )

        ProwlRuntime.storage.appendBlocking(finalLog)
        return response
    }

    private fun shouldCapture(request: Request): Boolean {
        val scheme = request.url.scheme.lowercase()
        if (scheme !in setOf("http", "https")) return false
        return !ProwlRuntime.shouldIgnore(request.url.toString())
    }

    private fun readRequestBody(request: Request): ByteArray? {
        val body = request.body ?: return null
        if (body.isDuplex() || body.isOneShot()) return null
        return runCatching {
            Buffer().use { buffer ->
                body.writeTo(buffer)
                buffer.readByteArray()
            }
        }.getOrNull()
    }

    private fun peekResponseBody(response: Response): ByteArray? =
        runCatching { response.peekBody(Long.MAX_VALUE).bytes() }.getOrNull()

    private fun buildMockResponse(request: Request, rule: com.prowllabs.prowl.core.mocking.ProwlMockRule): Response {
        val mediaType = rule.mockHeaders["Content-Type"]?.toMediaTypeOrNull()
        val body = rule.mockBody.toResponseBody(mediaType)
        val headersBuilder = Headers.Builder()
        rule.mockHeaders.forEach { (key, value) -> headersBuilder.add(key, value) }

        return Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(rule.mockStatusCode)
            .message("Mocked by Prowl")
            .headers(headersBuilder.build())
            .body(body)
            .sentRequestAtMillis(System.currentTimeMillis())
            .receivedResponseAtMillis(System.currentTimeMillis())
            .build()
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

    private fun Headers.toMap(): Map<String, String> =
        buildMap {
            for (index in 0 until size) {
                put(name(index), value(index))
            }
        }

}
