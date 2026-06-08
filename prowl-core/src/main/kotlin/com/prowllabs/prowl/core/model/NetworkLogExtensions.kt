package com.prowllabs.prowl.core.model

data class RequestTiming(
    val dnsMillis: Long? = null,
    val connectMillis: Long? = null,
    val secureConnectMillis: Long? = null,
    val requestHeadersMillis: Long? = null,
    val requestBodyMillis: Long? = null,
    val responseHeadersMillis: Long? = null,
    val responseBodyMillis: Long? = null,
) {
    val totalMillis: Long? = listOfNotNull(
        dnsMillis, connectMillis, secureConnectMillis,
        requestHeadersMillis, requestBodyMillis, responseHeadersMillis, responseBodyMillis,
    ).takeIf { it.isNotEmpty() }?.sum()
}

data class MultipartPart(
    val name: String? = null,
    val fileName: String? = null,
    val contentType: String? = null,
    val headers: Map<String, String> = emptyMap(),
    val sizeBytes: Int = 0,
    val textPreview: String? = null,
    val isBinary: Boolean = false,
)

enum class NetworkProtocol(val label: String) {
    HTTP("HTTP"),
    GRPC("gRPC"),
    WEBSOCKET("WebSocket"),
}
