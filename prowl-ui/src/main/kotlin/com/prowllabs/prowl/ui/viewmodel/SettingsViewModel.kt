package com.prowllabs.prowl.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prowllabs.prowl.core.mocking.ProwlMockRule
import com.prowllabs.prowl.core.runtime.ProwlRuntime
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class SettingsViewModel : ViewModel() {
    val logsCount: StateFlow<Int> =
        ProwlRuntime.storage.logsFlow
            .map { it.size }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val mockRuleCount: StateFlow<Int> =
        ProwlRuntime.mocker.rulesFlow
            .map { it.size }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val rewriteRuleCount: StateFlow<Int> =
        ProwlRuntime.requestRewriter.rulesFlow
            .map { it.size }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    var isLoggingEnabled: Boolean
        get() = ProwlRuntime.isLoggingEnabled
        set(value) { ProwlRuntime.isLoggingEnabled = value }

    var isMaskingEnabled: Boolean
        get() = ProwlRuntime.isSensitiveDataMaskingEnabled
        set(value) { ProwlRuntime.isSensitiveDataMaskingEnabled = value }

    fun allMockRules(): List<ProwlMockRule> = ProwlRuntime.mocker.allRules()

    fun addMockRule(rule: ProwlMockRule) = ProwlRuntime.mocker.addRule(rule)
}
