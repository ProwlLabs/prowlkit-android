package com.prowllabs.prowl.ui.util

import android.content.Context
import com.prowllabs.prowl.core.model.NetworkLog

/** Watch/pin endpoints — alerts when matching traffic appears. */
object ProwlWatchStore {
    private const val PREFS_NAME = "prowl_watch_endpoints"
    private const val KEY_PATTERNS = "patterns"

    fun endpointKey(log: NetworkLog): String = log.endpointKey()

    fun watchedPatterns(context: Context): Set<String> =
        prefs(context).getStringSet(KEY_PATTERNS, emptySet()).orEmpty()

    fun isWatched(context: Context, log: NetworkLog): Boolean =
        endpointKey(log) in watchedPatterns(context)

    fun isWatched(context: Context, key: String): Boolean =
        key in watchedPatterns(context)

    fun toggleWatch(context: Context, log: NetworkLog): Boolean {
        val key = endpointKey(log)
        val current = watchedPatterns(context).toMutableSet()
        val nowWatched = if (key in current) {
            current.remove(key)
            false
        } else {
            current.add(key)
            true
        }
        prefs(context).edit().putStringSet(KEY_PATTERNS, current).apply()
        return nowWatched
    }

    fun removeWatch(context: Context, key: String) {
        val current = watchedPatterns(context).toMutableSet()
        current.remove(key)
        prefs(context).edit().putStringSet(KEY_PATTERNS, current).apply()
    }

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}
