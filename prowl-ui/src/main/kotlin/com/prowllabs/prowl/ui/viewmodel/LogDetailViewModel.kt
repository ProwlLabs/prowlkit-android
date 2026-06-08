package com.prowllabs.prowl.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prowllabs.prowl.core.model.NetworkLog
import com.prowllabs.prowl.core.runtime.ProwlRuntime
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class LogDetailViewModel : ViewModel() {
    val logs: StateFlow<List<NetworkLog>> =
        ProwlRuntime.storage.logsFlow
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}
