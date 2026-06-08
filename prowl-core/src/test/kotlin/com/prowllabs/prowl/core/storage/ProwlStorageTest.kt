package com.prowllabs.prowl.core.storage

import com.prowllabs.prowl.core.makeLog
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class ProwlStorageTest {
    @Test
    fun appendBeyondLimit_keepsNewestLogs() = runBlocking {
        val storage = ProwlStorage(limit = 2)
        storage.append(makeLog(statusCode = 200, startedAtMillis = 1))
        storage.append(makeLog(statusCode = 201, startedAtMillis = 2))
        storage.append(makeLog(statusCode = 202, startedAtMillis = 3))

        val logs = storage.allLogs()
        assertEquals(2, logs.size)
        assertEquals(201, logs[0].statusCode)
        assertEquals(202, logs[1].statusCode)
    }

    @Test
    fun clear_removesAllLogs() = runBlocking {
        val storage = ProwlStorage()
        storage.append(makeLog())
        storage.clear()
        assertEquals(0, storage.allLogs().size)
    }

    @Test
    fun restoreIfEmptyBlocking_skipsWhenMemoryNotEmpty() {
        val storage = ProwlStorage()
        storage.appendBlocking(makeLog(statusCode = 100))
        storage.restoreIfEmptyBlocking(listOf(makeLog(statusCode = 999)))
        assertEquals(100, storage.allLogsBlocking().single().statusCode)
    }
}
