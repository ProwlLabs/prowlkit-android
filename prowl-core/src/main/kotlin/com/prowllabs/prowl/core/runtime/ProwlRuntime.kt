package com.prowllabs.prowl.core.runtime

import com.prowllabs.prowl.core.logging.ProwlEndpointRateAlerts
import com.prowllabs.prowl.core.logging.ResponseBodyLoggingTransformer
import com.prowllabs.prowl.core.masking.SensitiveDataMasker
import com.prowllabs.prowl.core.mocking.ProwlMocker
import com.prowllabs.prowl.core.storage.ProwlStorage

/** Global runtime configuration holder (mirrors iOS `ProwlRuntime`). */
object ProwlRuntime {
    @Volatile
    var isLoggingEnabled: Boolean = true

    @Volatile
    var isSensitiveDataMaskingEnabled: Boolean = false

    @Volatile
    var ignoredUrls: MutableSet<String> = linkedSetOf()

    @Volatile
    var ignoredUrlRegexes: MutableSet<String> = linkedSetOf()

    @Volatile
    var responseBodyLoggingTransformer: ResponseBodyLoggingTransformer? = null

    var storage: ProwlStorage = ProwlStorage()
        private set

    var masker: SensitiveDataMasker = SensitiveDataMasker()
        private set

    val mocker: ProwlMocker = ProwlMocker.shared

    fun configure(
        storage: ProwlStorage? = null,
        masker: SensitiveDataMasker? = null,
        isLoggingEnabled: Boolean? = null,
        isSensitiveDataMaskingEnabled: Boolean? = null,
    ) {
        storage?.let { this.storage = it }
        masker?.let { this.masker = it }
        isLoggingEnabled?.let { this.isLoggingEnabled = it }
        isSensitiveDataMaskingEnabled?.let { this.isSensitiveDataMaskingEnabled = it }
    }

    fun shouldIgnore(absoluteUrl: String): Boolean {
        if (ignoredUrls.any { absoluteUrl.contains(it) }) return true
        for (pattern in ignoredUrlRegexes) {
            runCatching {
                if (Regex(pattern).containsMatchIn(absoluteUrl)) return true
            }
        }
        return false
    }

    fun onLogsCleared() {
        ProwlEndpointRateAlerts.resetCounters()
    }
}
