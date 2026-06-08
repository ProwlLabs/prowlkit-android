package com.prowllabs.prowl.core.interceptor

import com.prowllabs.prowl.core.model.RequestTiming
import okhttp3.Call
import okhttp3.EventListener
import okhttp3.Protocol
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Proxy
import java.util.concurrent.ConcurrentHashMap

internal object ProwlTimingStore {
    private data class MutableTiming(
        var dnsStart: Long = 0,
        var dnsEnd: Long = 0,
        var connectStart: Long = 0,
        var connectEnd: Long = 0,
        var secureConnectStart: Long = 0,
        var secureConnectEnd: Long = 0,
        var requestHeadersStart: Long = 0,
        var requestHeadersEnd: Long = 0,
        var requestBodyStart: Long = 0,
        var requestBodyEnd: Long = 0,
        var responseHeadersStart: Long = 0,
        var responseHeadersEnd: Long = 0,
        var responseBodyStart: Long = 0,
        var responseBodyEnd: Long = 0,
    ) {
        fun toRequestTiming(): RequestTiming = RequestTiming(
            dnsMillis = diff(dnsStart, dnsEnd),
            connectMillis = diff(connectStart, connectEnd),
            secureConnectMillis = diff(secureConnectStart, secureConnectEnd),
            requestHeadersMillis = diff(requestHeadersStart, requestHeadersEnd),
            requestBodyMillis = diff(requestBodyStart, requestBodyEnd),
            responseHeadersMillis = diff(responseHeadersStart, responseHeadersEnd),
            responseBodyMillis = diff(responseBodyStart, responseBodyEnd),
        )

        private fun diff(start: Long, end: Long): Long? =
            if (start > 0 && end >= start) end - start else null
    }

    private val timings = ConcurrentHashMap<String, MutableTiming>()
    private val hostIps = ConcurrentHashMap<String, String>()

    fun keyFor(call: Call): String = System.identityHashCode(call).toString()

    fun remove(key: String) {
        timings.remove(key)
        hostIps.remove(key)
    }

    fun take(key: String): RequestTiming? =
        timings.remove(key)?.toRequestTiming()

    fun takeHostIp(key: String): String? = hostIps.remove(key)

    val eventListenerFactory: EventListener.Factory = EventListener.Factory { call ->
        val key = keyFor(call)
        timings[key] = MutableTiming()
        TimingEventListener(key)
    }

    private class TimingEventListener(
        private val key: String,
    ) : EventListener() {
        private fun mutator(block: MutableTiming.() -> Unit) {
            timings[key]?.block()
        }

        override fun dnsStart(call: Call, domainName: String) {
            mutator { dnsStart = System.currentTimeMillis() }
        }

        override fun dnsEnd(call: Call, domainName: String, inetAddressList: List<java.net.InetAddress>) {
            mutator { dnsEnd = System.currentTimeMillis() }
            inetAddressList.firstOrNull()?.hostAddress?.let { hostIps[key] = it }
        }

        override fun connectStart(call: Call, inetSocketAddress: InetSocketAddress, proxy: Proxy) {
            mutator { connectStart = System.currentTimeMillis() }
        }

        override fun connectEnd(
            call: Call,
            inetSocketAddress: InetSocketAddress,
            proxy: Proxy,
            protocol: Protocol?,
        ) {
            mutator { connectEnd = System.currentTimeMillis() }
        }

        override fun secureConnectStart(call: Call) {
            mutator { secureConnectStart = System.currentTimeMillis() }
        }

        override fun secureConnectEnd(call: Call, handshake: okhttp3.Handshake?) {
            mutator { secureConnectEnd = System.currentTimeMillis() }
        }

        override fun requestHeadersStart(call: Call) {
            mutator { requestHeadersStart = System.currentTimeMillis() }
        }

        override fun requestHeadersEnd(call: Call, request: okhttp3.Request) {
            mutator { requestHeadersEnd = System.currentTimeMillis() }
        }

        override fun requestBodyStart(call: Call) {
            mutator { requestBodyStart = System.currentTimeMillis() }
        }

        override fun requestBodyEnd(call: Call, byteCount: Long) {
            mutator { requestBodyEnd = System.currentTimeMillis() }
        }

        override fun responseHeadersStart(call: Call) {
            mutator { responseHeadersStart = System.currentTimeMillis() }
        }

        override fun responseHeadersEnd(call: Call, response: okhttp3.Response) {
            mutator { responseHeadersEnd = System.currentTimeMillis() }
        }

        override fun responseBodyStart(call: Call) {
            mutator { responseBodyStart = System.currentTimeMillis() }
        }

        override fun responseBodyEnd(call: Call, byteCount: Long) {
            mutator { responseBodyEnd = System.currentTimeMillis() }
        }

        override fun callFailed(call: Call, ioe: IOException) {
            timings.remove(key)
        }
    }
}
