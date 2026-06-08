package com.prowllabs.prowl.core.storage

import android.content.Context
import com.prowllabs.prowl.core.model.NetworkLog
import java.io.File
import java.util.concurrent.Executors

/** Persists captured logs to app-internal storage (survives process death). */
object ProwlSessionPersistence {
    private const val FILE_NAME = "prowl_session.json"
    private val executor = Executors.newSingleThreadExecutor()

    fun restoreBlocking(context: Context, storage: ProwlStorage) {
        val file = sessionFile(context)
        if (!file.exists()) return
        runCatching {
            val logs = NetworkLogSerializer.fromJson(file.readText())
            storage.restoreIfEmptyBlocking(logs)
        }
    }

    fun restoreAsync(context: Context, storage: ProwlStorage) {
        executor.execute {
            restoreBlocking(context, storage)
        }
    }

    fun persistAsync(context: Context, logs: List<NetworkLog>) {
        executor.execute {
            runCatching {
                sessionFile(context).writeText(NetworkLogSerializer.toJson(logs))
            }
        }
    }

    fun clearAsync(context: Context) {
        executor.execute {
            runCatching { sessionFile(context).delete() }
        }
    }

    private fun sessionFile(context: Context): File =
        File(context.filesDir, FILE_NAME)
}
