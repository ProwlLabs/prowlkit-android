package com.prowllabs.prowl.core.runtime

import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProwlRuntimeTest {
    @After
    fun tearDown() {
        ProwlRuntime.ignoredUrls = emptySet()
        ProwlRuntime.ignoredUrlRegexes = emptySet()
    }

    @Test
    fun shouldIgnore_matchesSubstringAndRegex() {
        ProwlRuntime.addIgnoredUrl("/health")
        ProwlRuntime.addIgnoredUrlRegex(".*\\.internal$")
        assertTrue(ProwlRuntime.shouldIgnore("https://api.example.com/health/check"))
        assertTrue(ProwlRuntime.shouldIgnore("https://metrics.internal"))
        assertFalse(ProwlRuntime.shouldIgnore("https://api.example.com/users"))
    }
}
