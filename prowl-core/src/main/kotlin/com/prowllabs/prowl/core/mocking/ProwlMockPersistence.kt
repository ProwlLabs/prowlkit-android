package com.prowllabs.prowl.core.mocking

import android.content.Context
import com.prowllabs.prowl.core.runtime.ProwlRuntime
import java.io.File
import java.util.concurrent.Executors

/** Persists mock rules so they survive process death. */
object ProwlMockPersistence {
    private const val FILE_NAME = "prowl_mock_rules.json"
    private val executor = Executors.newSingleThreadExecutor()

    fun restoreBlocking(mocker: ProwlMocker) {
        val context = ProwlRuntime.hostApplicationContext() ?: return
        val file = rulesFile(context)
        if (!file.exists()) return
        runCatching {
            val rules = ProwlMockExporter.importRules(file.readText())
            mocker.replaceAllRules(rules)
        }
    }

    fun persistAsync(rules: List<ProwlMockRule>) {
        val context = ProwlRuntime.hostApplicationContext() ?: return
        executor.execute {
            runCatching {
                rulesFile(context).writeText(ProwlMockExporter.exportRules(rules))
            }
        }
    }

    fun clearAsync() {
        val context = ProwlRuntime.hostApplicationContext() ?: return
        executor.execute {
            runCatching { rulesFile(context).delete() }
        }
    }

    private fun rulesFile(context: Context): File = File(context.filesDir, FILE_NAME)
}
