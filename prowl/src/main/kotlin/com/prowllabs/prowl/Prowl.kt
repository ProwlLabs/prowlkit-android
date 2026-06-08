package com.prowllabs.prowl

import android.app.Application
import android.content.Context
import com.prowllabs.prowl.core.interceptor.ProwlOkHttp
import com.prowllabs.prowl.core.logging.ProwlEndpointRateAlertRule
import com.prowllabs.prowl.core.logging.ResponseBodyLoggingTransformer
import com.prowllabs.prowl.core.masking.SensitiveDataMasker
import com.prowllabs.prowl.core.mocking.ProwlMockRule
import com.prowllabs.prowl.core.mocking.ProwlMocker
import com.prowllabs.prowl.core.runtime.ProwlRuntime
import com.prowllabs.prowl.core.storage.ProwlStorage
import com.prowllabs.prowl.ui.ProwlNotification
import com.prowllabs.prowl.ui.ProwlUiLauncher
import okhttp3.Interceptor
import java.util.UUID

/**
 * Main entry point for ProwlKit on Android.
 *
 * ```kotlin
 * class DemoApp : Application() {
 *     override fun onCreate() {
 *         super.onCreate()
 *         Prowl.start(this)
 *     }
 * }
 *
 * val client = OkHttpClient.Builder()
 *     .applyProwl()
 *     .build()
 * ```
 */
object Prowl {
    private var isRunning = false
    private var appContext: Context? = null

    val interceptor: Interceptor = ProwlOkHttp.interceptor

    var ignoredUrls: MutableSet<String>
        get() = ProwlRuntime.ignoredUrls
        set(value) { ProwlRuntime.ignoredUrls = value }

    var ignoredUrlRegexes: MutableSet<String>
        get() = ProwlRuntime.ignoredUrlRegexes
        set(value) { ProwlRuntime.ignoredUrlRegexes = value }

    var isLoggingEnabled: Boolean
        get() = ProwlRuntime.isLoggingEnabled
        set(value) { ProwlRuntime.isLoggingEnabled = value }

    var isSensitiveDataMaskingEnabled: Boolean
        get() = ProwlRuntime.isSensitiveDataMaskingEnabled
        set(value) { ProwlRuntime.isSensitiveDataMaskingEnabled = value }

    var responseBodyLoggingTransformer: ResponseBodyLoggingTransformer?
        get() = ProwlRuntime.responseBodyLoggingTransformer
        set(value) { ProwlRuntime.responseBodyLoggingTransformer = value }

    var endpointRateAlertRules: List<ProwlEndpointRateAlertRule>
        get() = com.prowllabs.prowl.core.logging.ProwlEndpointRateAlerts.rules
        set(value) { com.prowllabs.prowl.core.logging.ProwlEndpointRateAlerts.rules = value }

    fun storage(): ProwlStorage = ProwlRuntime.storage

    fun mocker(): ProwlMocker = ProwlRuntime.mocker

    fun mockRules(): List<ProwlMockRule> = ProwlRuntime.mocker.allRules()

    fun addMockRule(rule: ProwlMockRule) = ProwlRuntime.mocker.addRule(rule)

    fun updateMockRule(rule: ProwlMockRule) = ProwlRuntime.mocker.updateRule(rule)

    fun removeMockRule(id: UUID) = ProwlRuntime.mocker.removeRule(id)

    fun removeAllMockRules() = ProwlRuntime.mocker.removeAllRules()

    fun setMockRuleEnabled(id: UUID, enabled: Boolean) {
        val rule = ProwlRuntime.mocker.allRules().firstOrNull { it.id == id } ?: return
        ProwlRuntime.mocker.updateRule(rule.copy(isEnabled = enabled))
    }

    fun resetEndpointRateAlertCounters() {
        com.prowllabs.prowl.core.logging.ProwlEndpointRateAlerts.resetCounters()
    }

    fun ignoreUrl(urlSubstring: String) {
        ProwlRuntime.ignoredUrls.add(urlSubstring)
    }

    fun ignoreUrlRegex(pattern: String) {
        ProwlRuntime.ignoredUrlRegexes.add(pattern)
    }

    fun configure(
        storage: ProwlStorage? = null,
        masker: SensitiveDataMasker? = null,
        isLoggingEnabled: Boolean? = null,
        isSensitiveDataMaskingEnabled: Boolean? = null,
    ) {
        ProwlRuntime.configure(storage, masker, isLoggingEnabled, isSensitiveDataMaskingEnabled)
    }

    /**
     * Starts Prowl: enables the notification shortcut (Chucker-style) and marks
     * the library as active. Wire [interceptor] into your OkHttp client separately.
     */
    fun start(
        context: Context,
        ignoredUrls: List<String> = emptyList(),
        ignoredUrlRegexes: List<String> = emptyList(),
        showNotification: Boolean = true,
    ) {
        if (isRunning) return

        val appContext = context.applicationContext
        this.appContext = appContext
        ignoredUrls.forEach(::ignoreUrl)
        ignoredUrlRegexes.forEach(::ignoreUrlRegex)

        if (showNotification) {
            ProwlNotification.show(appContext)
        }
        isRunning = true
    }

    fun stop() {
        if (!isRunning) return
        appContext?.let { ProwlNotification.dismiss(it) }
        isRunning = false
    }

    fun show() {
        appContext?.let { ProwlUiLauncher.show(it) }
    }

    fun hide() {
        // Activity-based UI; user closes via back/close button.
    }

    fun toggle() {
        show()
    }
}

/** Convenience for OkHttp client builders. */
fun okhttp3.OkHttpClient.Builder.applyProwl(): okhttp3.OkHttpClient.Builder =
    addInterceptor(Prowl.interceptor)
