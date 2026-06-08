package com.prowllabs.prowl.core.interceptor

import okhttp3.OkHttpClient

object ProwlOkHttp {
    val interceptor: ProwlInterceptor = ProwlInterceptor()

    fun OkHttpClient.Builder.applyProwl(): OkHttpClient.Builder =
        addNetworkInterceptor(interceptor)
            .eventListenerFactory(ProwlTimingStore.eventListenerFactory)
}
