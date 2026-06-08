package com.prowllabs.prowl.core.storage

import com.prowllabs.prowl.core.model.NetworkLog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Thread-safe FIFO buffer of captured [NetworkLog] entries. */
class ProwlStorage(
    limit: Int = DEFAULT_LIMIT,
) {
    private val mutex = Mutex()
    private var logs = mutableListOf<NetworkLog>()
    private var limit = limit.coerceAtLeast(1)

    private val _logsFlow = MutableStateFlow<List<NetworkLog>>(emptyList())
    val logsFlow: StateFlow<List<NetworkLog>> = _logsFlow.asStateFlow()

    fun setLimit(newLimit: Int) {
        synchronized(this) {
            limit = newLimit.coerceAtLeast(1)
            trimToLimit()
            publish()
        }
    }

    suspend fun append(log: NetworkLog) {
        mutex.withLock {
            logs.add(log)
            trimToLimit()
            publish()
        }
    }

    fun appendBlocking(log: NetworkLog) {
        synchronized(this) {
            logs.add(log)
            trimToLimit()
            publish()
        }
    }

    suspend fun allLogs(): List<NetworkLog> = mutex.withLock { logs.toList() }

    fun allLogsBlocking(): List<NetworkLog> = synchronized(this) { logs.toList() }

    suspend fun clear() {
        mutex.withLock {
            logs.clear()
            publish()
        }
    }

    fun clearBlocking() {
        synchronized(this) {
            logs.clear()
            publish()
        }
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
