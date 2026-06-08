package com.prowllabs.prowl.ui.internal

import android.app.Activity
import java.lang.ref.WeakReference

internal object ProwlActivityTracker {
    private val lock = Any()
    private var visibleActivity: WeakReference<Activity>? = null

    fun onResumed(activity: Activity) = synchronized(lock) {
        visibleActivity = WeakReference(activity)
    }

    fun onPaused(activity: Activity) = synchronized(lock) {
        if (visibleActivity?.get() === activity) {
            visibleActivity = null
        }
    }

    fun isInspectorVisible(): Boolean = synchronized(lock) {
        visibleActivity?.get() != null
    }

    fun hideInspector() = synchronized(lock) {
        visibleActivity?.get()?.finish()
    }
}
