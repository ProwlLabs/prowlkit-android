package com.prowllabs.prowl.core.interceptor

import okhttp3.OkHttpClient

/** Helpers for wiring Prowl into OkHttp clients. */
object ProwlOkHttp {
    val interceptor: ProwlInterceptor = ProwlInterceptor()

    fun OkHttpClient.Builder.applyProwl(): OkHttpClient.Builder =
        addInterceptor(interceptor)
}
