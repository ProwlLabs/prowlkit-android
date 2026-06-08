package com.prowllabs.prowl.core.logging

import com.prowllabs.prowl.core.makeLog
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProwlEndpointRateAlertsTest {
    @After
    fun tearDown() {
        ProwlEndpointRateAlerts.resetCounters()
        ProwlEndpointRateAlerts.rules = emptyList()
    }

    @Test
    fun evaluate_triggersOnlyOnConfiguredThreshold() {
        ProwlEndpointRateAlerts.rules = listOf(
            ProwlEndpointRateAlertRule(
                match = ProwlEndpointRateAlertRule.Match.UrlContains("/users"),
                threshold = 2,
            ),
        )
        val log = makeLog(url = "https://api.example.com/users")
        assertFalse(ProwlEndpointRateAlerts.evaluate(log))
        assertTrue(ProwlEndpointRateAlerts.evaluate(log))
    }
}
