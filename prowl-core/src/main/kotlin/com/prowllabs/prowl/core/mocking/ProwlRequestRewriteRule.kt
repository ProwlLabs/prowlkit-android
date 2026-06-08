package com.prowllabs.prowl.core.mocking

import java.util.UUID

/**
 * Rewrites outgoing requests that match [targetUrlPattern] before they reach the network.
 * Response mock rules (short-circuit) are separate — see [ProwlMockRule].
 */
data class ProwlRequestRewriteRule(
    val id: UUID = UUID.randomUUID(),
    val targetUrlPattern: String,
    val targetMethod: String = "ANY",
    /** Full URL (https://…) or path (/v2/users). Blank = leave URL unchanged. */
    val replacementUrl: String = "",
    val headerOverrides: Map<String, String> = emptyMap(),
    val headersToRemove: Set<String> = emptySet(),
    /** Null = leave body unchanged. Empty array = clear body. */
    val replacementBody: ByteArray? = null,
    val replacementContentType: String? = null,
    val isEnabled: Boolean = true,
) {
    val replacementBodyText: String?
        get() = replacementBody?.toString(Charsets.UTF_8)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ProwlRequestRewriteRule) return false
        return id == other.id &&
            targetUrlPattern == other.targetUrlPattern &&
            targetMethod == other.targetMethod &&
            replacementUrl == other.replacementUrl &&
            headerOverrides == other.headerOverrides &&
            headersToRemove == other.headersToRemove &&
            replacementBody.contentEqualsOrBothNull(other.replacementBody) &&
            replacementContentType == other.replacementContentType &&
            isEnabled == other.isEnabled
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + targetUrlPattern.hashCode()
        result = 31 * result + targetMethod.hashCode()
        result = 31 * result + replacementUrl.hashCode()
        result = 31 * result + headerOverrides.hashCode()
        result = 31 * result + headersToRemove.hashCode()
        result = 31 * result + (replacementBody?.contentHashCode() ?: 0)
        result = 31 * result + (replacementContentType?.hashCode() ?: 0)
        result = 31 * result + isEnabled.hashCode()
        return result
    }

    private fun ByteArray?.contentEqualsOrBothNull(other: ByteArray?): Boolean =
        when {
            this === other -> true
            this == null || other == null -> false
            else -> contentEquals(other)
        }
}
