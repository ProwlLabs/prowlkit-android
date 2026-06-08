package com.prowllabs.prowl.core.model

import java.util.UUID

/** A single captured network round-trip with request, response, and metadata. */
data class NetworkLog(
    val id: UUID = UUID.randomUUID(),
    val requestId: UUID = UUID.randomUUID(),
    val url: String?,
    val method: String,
    val requestHeaders: Map<String, String> = emptyMap(),
    val requestBody: Body? = null,
    val responseHeaders: Map<String, String> = emptyMap(),
    val responseBody: Body? = null,
    val statusCode: Int? = null,
    val startedAtMillis: Long,
    val durationMillis: Long,
    val timeoutMillis: Long? = null,
    val cachePolicy: String? = null,
    val errorDescription: String? = null,
    val endpointRateAlertTriggered: Boolean = false,
) {
    data class Body(
        val data: ByteArray,
        val contentType: String? = null,
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Body) return false
            return data.contentEquals(other.data) && contentType == other.contentType
        }

        override fun hashCode(): Int = 31 * data.contentHashCode() + (contentType?.hashCode() ?: 0)
    }
}
