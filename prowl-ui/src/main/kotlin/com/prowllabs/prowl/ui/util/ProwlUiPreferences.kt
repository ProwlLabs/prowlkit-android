package com.prowllabs.prowl.ui.util

import android.content.Context
import com.prowllabs.prowl.core.runtime.ProwlRuntime

enum class ProwlThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
}

object ProwlUiPreferences {
    private const val PREFS_NAME = "prowl_ui_preferences"
    private const val KEY_SHAKE_TO_CLEAR = "shake_to_clear_logs"
    private const val KEY_SHAKE_TO_OPEN = "shake_to_open_inspector"
    private const val KEY_THEME_MODE = "theme_mode"
    private const val KEY_PERSIST_SESSIONS = "persist_sessions"
    private const val KEY_FLOATING_BUBBLE = "floating_bubble"

    private fun prefs(context: Context) =
        (ProwlRuntime.hostApplicationContext() ?: context.applicationContext)
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun isShakeToClearEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_SHAKE_TO_CLEAR, false)

    fun setShakeToClearEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_SHAKE_TO_CLEAR, enabled).apply()
    }

    fun isShakeToOpenEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_SHAKE_TO_OPEN, false)

    fun setShakeToOpenEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_SHAKE_TO_OPEN, enabled).apply()
    }

    fun themeMode(context: Context): ProwlThemeMode {
        val raw = prefs(context).getString(KEY_THEME_MODE, ProwlThemeMode.SYSTEM.name)
        return runCatching { ProwlThemeMode.valueOf(raw ?: ProwlThemeMode.SYSTEM.name) }
            .getOrElse { ProwlThemeMode.SYSTEM }
    }

    fun setThemeMode(context: Context, mode: ProwlThemeMode) {
        prefs(context).edit().putString(KEY_THEME_MODE, mode.name).apply()
    }

    fun isSessionPersistenceEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_PERSIST_SESSIONS, false)

    fun setSessionPersistenceEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_PERSIST_SESSIONS, enabled).apply()
        com.prowllabs.prowl.core.runtime.ProwlRuntime.isSessionPersistenceEnabled = enabled
        if (enabled) {
            com.prowllabs.prowl.core.runtime.ProwlRuntime.restorePersistedSession()
        }
    }

    fun isFloatingBubbleEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_FLOATING_BUBBLE, false)

    fun setFloatingBubbleEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_FLOATING_BUBBLE, enabled).apply()
    }
}
