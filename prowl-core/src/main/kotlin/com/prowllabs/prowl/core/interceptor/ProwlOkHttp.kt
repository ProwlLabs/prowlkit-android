package com.prowllabs.prowl.core.interceptor

import okhttp3.OkHttpClient

object ProwlOkHttp {
    val interceptor: ProwlInterceptor = ProwlInterceptor()

    fun OkHttpClient.Builder.applyProwl(): OkHttpClient.Builder =
        // Application interceptor (same as Chucker) — safer for body capture and mock short-circuit.
        addInterceptor(interceptor)
            .eventListenerFactory(ProwlTimingStore.eventListenerFactory)
}
