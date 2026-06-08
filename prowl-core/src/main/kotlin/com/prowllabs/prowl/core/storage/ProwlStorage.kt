package com.prowllabs.prowl.core.storage

import com.prowllabs.prowl.core.model.NetworkLog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ProwlStorage(
    limit: Int = DEFAULT_LIMIT,
) {
    private val lock = Any()
    private var logs = mutableListOf<NetworkLog>()
    private var limit = limit.coerceAtLeast(1)

    private val _logsFlow = MutableStateFlow<List<NetworkLog>>(emptyList())
    val logsFlow: StateFlow<List<NetworkLog>> = _logsFlow.asStateFlow()

    fun setLimit(newLimit: Int) = synchronized(lock) {
        limit = newLimit.coerceAtLeast(1)
        trimToLimit()
        publish()
    }

    suspend fun append(log: NetworkLog) = synchronized(lock) {
        logs.add(log)
        trimToLimit()
        publish()
    }

    fun appendBlocking(log: NetworkLog) = synchronized(lock) {
        logs.add(log)
        trimToLimit()
        publish()
    }

    suspend fun allLogs(): List<NetworkLog> = synchronized(lock) { logs.toList() }

    fun allLogsBlocking(): List<NetworkLog> = synchronized(lock) { logs.toList() }

    suspend fun clear() = synchronized(lock) {
        logs.clear()
        publish()
    }

    fun clearBlocking() = synchronized(lock) {
        logs.clear()
        publish()
    }

    fun replaceAllBlocking(newLogs: List<NetworkLog>) = synchronized(lock) {
        logs.clear()
        logs.addAll(newLogs)
        trimToLimit()
        publish()
    }

    /** Restores persisted session only when memory is still empty (avoids wiping live captures). */
    fun restoreIfEmptyBlocking(newLogs: List<NetworkLog>) = synchronized(lock) {
        if (logs.isNotEmpty()) return
        logs.addAll(newLogs)
        trimToLimit()
        publish()
    }

    private fun trimToLimit() {
        if (logs.size > limit) {
            val overflow = logs.size - limit
            repeat(overflow) { logs.removeAt(0) }
        }
    }

    private fun publish() {
        _logsFlow.value = logs.toList()
    }

    companion object {
        const val DEFAULT_LIMIT = 200
    }
}
