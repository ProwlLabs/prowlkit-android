package com.prowllabs.prowl.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prowllabs.prowl.core.mocking.ProwlMockRule
import com.prowllabs.prowl.core.mocking.ProwlRequestRewriteRule
import com.prowllabs.prowl.core.runtime.ProwlRuntime
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import java.util.UUID

class MocksViewModel : ViewModel() {
    val mockRules: StateFlow<List<ProwlMockRule>> =
        ProwlRuntime.mocker.rulesFlow
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val rewriteRules: StateFlow<List<ProwlRequestRewriteRule>> =
        ProwlRuntime.requestRewriter.rulesFlow
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setMockEnabled(rule: ProwlMockRule, enabled: Boolean) {
        ProwlRuntime.mocker.updateRule(rule.copy(isEnabled = enabled))
    }

    fun deleteMock(id: UUID) {
        ProwlRuntime.mocker.removeRule(id)
    }

    fun deleteAllMocks() {
        ProwlRuntime.mocker.removeAllRules()
    }

    fun setRewriteEnabled(rule: ProwlRequestRewriteRule, enabled: Boolean) {
        ProwlRuntime.requestRewriter.updateRule(rule.copy(isEnabled = enabled))
    }

    fun deleteRewrite(id: UUID) {
        ProwlRuntime.requestRewriter.removeRule(id)
    }

    fun deleteAllRewrites() {
        ProwlRuntime.requestRewriter.removeAllRules()
    }
}
