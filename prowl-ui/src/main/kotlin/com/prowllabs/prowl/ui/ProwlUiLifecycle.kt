package com.prowllabs.prowl.ui

import android.app.Application
import android.content.Context
import com.prowllabs.prowl.ui.internal.ProwlActivityTracker
import com.prowllabs.prowl.ui.util.ProwlFloatingBubble
import com.prowllabs.prowl.ui.util.ProwlGlobalShakeMonitor

/** Lifecycle hooks used by the [com.prowllabs.prowl.Prowl] facade. */
object ProwlUiLifecycle {
    fun install(context: Context, showNotification: Boolean) {
        val application = context.applicationContext
        if (application is Application) {
            ProwlGlobalShakeMonitor.install(application)
            ProwlFloatingBubble.install(application)
        }
        if (showNotification) {
            ProwlNotification.show(context)
        }
    }

    fun uninstall(context: Context) {
        ProwlNotification.dismiss(context)
        ProwlGlobalShakeMonitor.uninstall()
        ProwlFloatingBubble.uninstall()
    }

    fun isInspectorVisible(): Boolean = ProwlActivityTracker.isInspectorVisible()

    fun hideInspector() = ProwlActivityTracker.hideInspector()
}
