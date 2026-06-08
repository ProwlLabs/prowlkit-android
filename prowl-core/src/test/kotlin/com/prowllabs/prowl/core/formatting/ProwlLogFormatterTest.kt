package com.prowllabs.prowl.core.formatting

import com.prowllabs.prowl.core.makeLog
import com.prowllabs.prowl.core.model.NetworkLog
import org.junit.Assert.assertTrue
import org.junit.Test

class ProwlLogFormatterTest {
    @Test
    fun curlCommand_includesMethodHeadersAndUrl() {
        val log = makeLog().copy(
            requestHeaders = mapOf("Accept" to "application/json"),
            requestBody = NetworkLog.Body("""{"id":1}""".toByteArray(), "application/json"),
        )
        val curl = ProwlLogFormatter.curlCommand(log)
        assertTrue(curl.contains("curl -X GET"))
        assertTrue(curl.contains("Accept: application/json"))
        assertTrue(curl.contains("https://api.example.com/users"))
    }

    @Test
    fun formattedExport_containsRequestAndResponseSections() {
        val text = ProwlLogFormatter.export(listOf(makeLog()), ProwlExportFormat.FORMATTED_TEXT)
        assertTrue(text.contains("*REQUEST*"))
        assertTrue(text.contains("*RESPONSE*"))
        assertTrue(text.contains("GET"))
    }
}
