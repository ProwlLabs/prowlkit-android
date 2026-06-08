package com.prowllabs.prowl.core.interceptor

import com.prowllabs.prowl.core.mocking.ProwlMockRule
import com.prowllabs.prowl.core.mocking.ProwlMocker
import com.prowllabs.prowl.core.runtime.ProwlRuntime
import com.prowllabs.prowl.core.storage.ProwlStorage
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ProwlInterceptorMockTest {
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
    fun mockShortCircuit_returnsMockBodyWithoutHittingServer() {
        server.enqueue(MockResponse().setBody("{\"real\":true}"))

        ProwlRuntime.mocker.addRule(
            ProwlMockRule(
                targetUrlPattern = server.hostName,
                targetMethod = "GET",
                mockStatusCode = 200,
                mockBody = """{"mocked":true}""".toByteArray(),
            ),
        )

        val response = client.newCall(
            Request.Builder().url(server.url("/api/test")).build(),
        ).execute()

        response.use {
            assertEquals(200, it.code)
            assertEquals("""{"mocked":true}""", it.body?.string())
        }
        assertEquals(0, server.requestCount)
        assertEquals(1, ProwlRuntime.storage.allLogsBlocking().size)
    }

    @Test
    fun mockWithPostBody_replaysRequestBodyWithoutCrash() {
        server.enqueue(MockResponse().setBody("{\"real\":true}"))

        ProwlRuntime.mocker.addRule(
            ProwlMockRule(
                targetUrlPattern = "/api/post",
                targetMethod = "POST",
                mockStatusCode = 200,
                mockBody = """{"ok":true}""".toByteArray(),
            ),
        )

        val response = client.newCall(
            Request.Builder()
                .url(server.url("/api/post"))
                .post("""{"input":1}""".toRequestBody("application/json".toMediaType()))
                .build(),
        ).execute()

        response.use {
            assertEquals(200, it.code)
            assertEquals("""{"ok":true}""", it.body?.string())
        }
        assertEquals(0, server.requestCount)
    }

    @Test
    fun refreshLikeGetSequence_handlesMultipleMockedCalls() {
        repeat(3) {
            server.enqueue(MockResponse().setBody("{\"real\":true}"))
        }

        ProwlRuntime.mocker.addRule(
            ProwlMockRule(
                targetUrlPattern = "/feed",
                mockStatusCode = 200,
                mockBody = """{"items":[]}""".toByteArray(),
            ),
        )

        repeat(3) {
            client.newCall(
                Request.Builder().url(server.url("/feed?page=$it")).build(),
            ).execute().use { response ->
                assertEquals(200, response.code)
                assertEquals("""{"items":[]}""", response.body?.string())
            }
        }

        assertEquals(0, server.requestCount)
        assertEquals(3, ProwlRuntime.storage.allLogsBlocking().size)
    }

    @Test
    fun mockNeverCrashesHost_evenWithInvalidStoredHeaders() {
        ProwlRuntime.mocker.addRule(
            ProwlMockRule(
                targetUrlPattern = "/broken",
                mockStatusCode = 200,
                mockBody = "{}".toByteArray(),
                mockHeaders = mapOf(
                    "Content-Type" to "application/json",
                    "Content-Length" to "99999",
                    "Content-Encoding" to "gzip",
                ),
            ),
        )

        val response = client.newCall(
            Request.Builder().url(server.url("/broken")).build(),
        ).execute()

        response.use {
            assertTrue(it.isSuccessful)
            assertEquals("{}", it.body?.string())
        }
    }
}
