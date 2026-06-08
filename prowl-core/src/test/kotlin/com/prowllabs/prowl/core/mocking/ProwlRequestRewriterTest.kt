package com.prowllabs.prowl.core.mocking

import okhttp3.Request
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class ProwlRequestRewriterTest {
    private lateinit var rewriter: ProwlRequestRewriter

    @Before
    fun setUp() {
        rewriter = ProwlRequestRewriter()
    }

    @Test
    fun findMatch_respectsMethodFilter() {
        rewriter.addRule(
            ProwlRequestRewriteRule(
                targetUrlPattern = "/users",
                targetMethod = "POST",
                replacementUrl = "/v2/users",
            ),
        )
        assertNotNull(rewriter.findMatch("https://api.example.com/users", "POST"))
        assertNull(rewriter.findMatch("https://api.example.com/users", "GET"))
    }

    @Test
    fun apply_rewritesUrlAndHeaders() {
        val rule = ProwlRequestRewriteRule(
            targetUrlPattern = "/users",
            replacementUrl = "/v2/users",
            headerOverrides = mapOf("X-Test" to "1"),
            headersToRemove = setOf("Authorization"),
        )
        val request = Request.Builder()
            .url("https://api.example.com/users")
            .header("Authorization", "secret")
            .get()
            .build()

        val rewritten = rewriter.apply(request, rule)
        assertEquals("https://api.example.com/v2/users", rewritten.url.toString())
        assertEquals("1", rewritten.header("X-Test"))
        assertNull(rewritten.header("Authorization"))
    }
}
