package com.prowllabs.prowl.ui.viewmodel

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prowllabs.prowl.core.model.NetworkLog
import com.prowllabs.prowl.core.runtime.ProwlRuntime
import com.prowllabs.prowl.ui.util.ProwlSearchParser
import com.prowllabs.prowl.ui.util.ProwlWatchStore
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class InspectorViewModel : ViewModel() {
    val logs: StateFlow<List<NetworkLog>> =
        ProwlRuntime.storage.logsFlow
            .map { entries -> entries.sortedByDescending { it.startedAtMillis } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    var searchText by mutableStateOf("")
        private set

    fun updateSearchText(value: String) {
        searchText = value
    }

    fun clearLogs() {
        viewModelScope.launch {
            ProwlRuntime.storage.clear()
            ProwlRuntime.onLogsCleared()
        }
    }

    fun filterLogs(context: Context, logs: List<NetworkLog>): List<NetworkLog> {
        val query = ProwlSearchParser.parse(searchText)
        return logs
            .filter { ProwlSearchParser.matches(it, query) }
            .sortedWith(
                compareByDescending<NetworkLog> { ProwlWatchStore.isWatched(context, it) }
                    .thenByDescending { it.startedAtMillis },
            )
    }
}
