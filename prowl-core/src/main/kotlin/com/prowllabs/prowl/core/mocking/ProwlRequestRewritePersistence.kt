package com.prowllabs.prowl.core.mocking

import android.content.Context
import com.prowllabs.prowl.core.runtime.ProwlRuntime
import java.io.File
import java.util.concurrent.Executors

/** Persists request rewrite rules so they survive process death. */
object ProwlRequestRewritePersistence {
    private const val FILE_NAME = "prowl_request_rewrite_rules.json"
    private val executor = Executors.newSingleThreadExecutor()

    fun restoreBlocking(rewriter: ProwlRequestRewriter) {
        val context = ProwlRuntime.hostApplicationContext() ?: return
        val file = rulesFile(context)
        if (!file.exists()) return
        runCatching {
            val rules = ProwlRequestRewriteExporter.importRules(file.readText())
            rewriter.replaceAllRules(rules)
        }
    }

    fun persistAsync(rules: List<ProwlRequestRewriteRule>) {
        val context = ProwlRuntime.hostApplicationContext() ?: return
        executor.execute {
            runCatching {
                rulesFile(context).writeText(ProwlRequestRewriteExporter.exportRules(rules))
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
