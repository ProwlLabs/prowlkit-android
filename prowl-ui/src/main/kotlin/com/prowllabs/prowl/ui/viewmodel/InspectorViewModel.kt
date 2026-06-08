package com.prowllabs.prowl.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prowllabs.prowl.core.model.NetworkLog
import com.prowllabs.prowl.core.runtime.ProwlRuntime
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class InspectorViewModel : ViewModel() {
    val logs: StateFlow<List<NetworkLog>> =
        ProwlRuntime.storage.logsFlow
            .map { it.reversed() }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val isLoggingEnabled: Boolean
        get() = ProwlRuntime.isLoggingEnabled

    val isSensitiveDataMaskingEnabled: Boolean
        get() = ProwlRuntime.isSensitiveDataMaskingEnabled

    fun setLoggingEnabled(enabled: Boolean) {
        ProwlRuntime.isLoggingEnabled = enabled
    }

    fun setSensitiveDataMaskingEnabled(enabled: Boolean) {
        ProwlRuntime.isSensitiveDataMaskingEnabled = enabled
    }

    fun clearLogs() {
        viewModelScope.launch {
            ProwlRuntime.storage.clear()
            ProwlRuntime.onLogsCleared()
        }
    }
}
