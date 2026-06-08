package com.prowllabs.prowl.core.mocking

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProwlRuleMatcherTest {
    @Test
    fun matches_respectsEnabledFlagAndMethod() {
        assertTrue(
            ProwlRuleMatcher.matches(
                url = "https://api.example.com/users",
                method = "GET",
                targetUrlPattern = "/users",
                targetMethod = "GET",
                isEnabled = true,
            ),
        )
        assertFalse(
            ProwlRuleMatcher.matches(
                url = "https://api.example.com/users",
                method = "POST",
                targetUrlPattern = "/users",
                targetMethod = "GET",
                isEnabled = true,
            ),
        )
        assertTrue(
            ProwlRuleMatcher.matches(
                url = "https://api.example.com/users",
                method = "POST",
                targetUrlPattern = "/users",
                targetMethod = "ANY",
                isEnabled = true,
            ),
        )
        assertFalse(
            ProwlRuleMatcher.matches(
                url = "https://api.example.com/users",
                method = "GET",
                targetUrlPattern = "/users",
                targetMethod = "GET",
                isEnabled = false,
            ),
        )
    }
}
