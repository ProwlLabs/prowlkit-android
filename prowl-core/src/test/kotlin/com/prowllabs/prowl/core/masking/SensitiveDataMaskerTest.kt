package com.prowllabs.prowl.core.masking

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SensitiveDataMaskerTest {
    @Test
    fun maskHeaders_redactsConfiguredHeadersCaseInsensitively() {
        val masker = SensitiveDataMasker(sensitiveHeaders = setOf("authorization"))
        val masked = masker.mask(
            mapOf(
                "Authorization" to "Bearer secret",
                "Content-Type" to "application/json",
            ),
        )
        assertEquals("[REDACTED]", masked["Authorization"])
        assertEquals("application/json", masked["Content-Type"])
    }

    @Test
    fun maskTextBody_redactsBearerTokens() {
        val masker = SensitiveDataMasker()
        val masked = masker.mask(
            body = "Authorization: Bearer secret-token".toByteArray(),
            contentType = "text/plain",
        )
        assertNotNull(masked)
        assertTrue(String(masked!!.data).contains("[REDACTED]"))
        assertTrue(!String(masked.data).contains("secret-token"))
    }
}
