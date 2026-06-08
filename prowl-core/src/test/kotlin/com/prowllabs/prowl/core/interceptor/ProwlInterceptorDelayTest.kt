package com.prowllabs.prowl.core.interceptor

import com.prowllabs.prowl.core.mocking.ProwlMockRule
import com.prowllabs.prowl.core.runtime.ProwlRuntime
import com.prowllabs.prowl.core.storage.ProwlStorage
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ProwlInterceptorDelayTest {
    private lateinit var server: MockWebServer
    private lateinit var client: OkHttpClient

    @Before
    fun setUp() {
        ProwlRuntime.configure(storage = ProwlStorage(), isLoggingEnabled = true)
        ProwlRuntime.mocker.removeAllRules()
        server = MockWebServer()
        server.start()
        client = OkHttpClient.Builder()
            .addInterceptor(ProwlInterceptor())
            .build()
    }

    @After
    fun tearDown() {
        ProwlRuntime.mocker.removeAllRules()
        server.shutdown()
    }

    @Test
    fun mockResponse_honorsConfiguredDelay() {
        ProwlRuntime.mocker.saveRule(
            ProwlMockRule(
                targetUrlPattern = server.hostName,
                targetMethod = "GET",
                mockStatusCode = 200,
                mockBody = """{"delayed":true}""".toByteArray(),
                responseDelayMillis = 300,
            ),
        )

        val started = System.currentTimeMillis()
        val response = client.newCall(
            Request.Builder().url(server.url("/delayed")).build(),
        ).execute()
        val elapsed = System.currentTimeMillis() - started

        response.close()
        assertTrue(elapsed >= 250)
        assertEquals(0, server.requestCount)
    }
}
