package com.prowllabs.prowl.core.websocket

import com.prowllabs.prowl.core.model.NetworkLog
import com.prowllabs.prowl.core.model.NetworkProtocol
import com.prowllabs.prowl.core.runtime.ProwlRuntime
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import java.util.UUID

/**
 * Wraps a [WebSocketListener] to capture WebSocket events into [ProwlRuntime.storage].
 *
 * ```kotlin
 * client.newWebSocket(request, ProwlWebSocketListener(delegate, request))
 * ```
 */
class ProwlWebSocketListener(
    private val delegate: WebSocketListener,
    private val request: Request,
) : WebSocketListener() {
    private val connectionId = UUID.randomUUID()
    private val startedAt = System.currentTimeMillis()

    override fun onOpen(webSocket: WebSocket, response: Response) {
        log("OPEN", response.code, response.message, response.headers.toMap(), null)
        delegate.onOpen(webSocket, response)
    }

    override fun onMessage(webSocket: WebSocket, text: String) {
        log("MESSAGE", 200, "text frame", emptyMap(), text)
        delegate.onMessage(webSocket, text)
    }

    override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
        log("MESSAGE", 200, "binary frame (${bytes.size} bytes)", emptyMap(), bytes.previewHex())
        delegate.onMessage(webSocket, bytes)
    }

    override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
        log("CLOSING", code, reason, emptyMap(), null)
        delegate.onClosing(webSocket, code, reason)
    }

    override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
        log("CLOSED", code, reason, emptyMap(), null)
        delegate.onClosed(webSocket, code, reason)
    }

    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
        log(
            "FAILURE",
            response?.code ?: 0,
            t.message ?: "WebSocket failure",
            response?.headers?.toMap().orEmpty(),
            null,
        )
        delegate.onFailure(webSocket, t, response)
    }

    private fun log(
        event: String,
        code: Int,
        message: String,
        headers: Map<String, String>,
        payload: String?,
    ) {
        if (!ProwlRuntime.isLoggingEnabled) return
        val now = System.currentTimeMillis()
        val log = NetworkLog(
            requestId = connectionId,
            url = request.url.toString(),
            method = "WS_$event",
            requestHeaders = request.headers.toMap(),
            responseHeaders = headers,
            responseBody = payload?.let {
                NetworkLog.Body(it.toByteArray(Charsets.UTF_8), "text/plain")
            },
            statusCode = code.takeIf { it > 0 },
            startedAtMillis = startedAt,
            durationMillis = now - startedAt,
            errorDescription = if (event == "FAILURE") message else null,
            protocol = NetworkProtocol.WEBSOCKET,
        )
        ProwlRuntime.storage.appendBlocking(log)
        ProwlRuntime.onLogsChanged()
    }

    private fun okhttp3.Headers.toMap(): Map<String, String> = buildMap {
        for (i in 0 until size) {
            put(name(i), value(i))
        }
    }
}

private const val MAX_BINARY_HEX_BYTES = 256

private fun ByteString.previewHex(): String {
    if (size <= MAX_BINARY_HEX_BYTES) return hex()
    val preview = substring(0, MAX_BINARY_HEX_BYTES).hex()
    return "$preview… (${size} bytes total)"
}

/** Convenience for wiring WebSocket logging. */
object ProwlWebSocket {
    fun listener(delegate: WebSocketListener, request: Request): WebSocketListener =
        ProwlWebSocketListener(delegate, request)
}
