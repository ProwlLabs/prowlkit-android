package com.prowllabs.prowl.ui.util

import android.app.Application
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.prowllabs.prowl.ui.ProwlUiLauncher
import kotlin.math.sqrt

object ProwlGlobalShakeMonitor : SensorEventListener {
    private var sensorManager: SensorManager? = null
    private var accelerometer: Sensor? = null
    private var lastShakeAt = 0L
    private var foregroundActivities = 0

    fun install(application: Application) {
        if (sensorManager != null) return
        sensorManager = application.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        application.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
            override fun onActivityResumed(activity: android.app.Activity) {
                foregroundActivities++
                registerSensor()
            }

            override fun onActivityPaused(activity: android.app.Activity) {
                foregroundActivities = (foregroundActivities - 1).coerceAtLeast(0)
                if (foregroundActivities == 0) unregisterSensor()
            }

            override fun onActivityCreated(a: android.app.Activity, b: android.os.Bundle?) = Unit
            override fun onActivityStarted(a: android.app.Activity) = Unit
            override fun onActivityStopped(a: android.app.Activity) = Unit
            override fun onActivitySaveInstanceState(a: android.app.Activity, b: android.os.Bundle) = Unit
            override fun onActivityDestroyed(a: android.app.Activity) = Unit
        })
    }

    private fun registerSensor() {
        val sm = sensorManager ?: return
        val sensor = accelerometer ?: return
        sm.registerListener(this, sensor, SensorManager.SENSOR_DELAY_UI)
    }

    private fun unregisterSensor() {
        sensorManager?.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        val context = com.prowllabs.prowl.core.runtime.ProwlRuntime.hostApplicationContext() ?: return
        val shakeToOpen = ProwlUiPreferences.isShakeToOpenEnabled(context)
        val shakeToClear = ProwlUiPreferences.isShakeToClearEnabled(context)
        if (!shakeToOpen && !shakeToClear) return

        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]
        val gForce = sqrt((x * x + y * y + z * z).toDouble()) / SensorManager.GRAVITY_EARTH
        if (gForce <= SHAKE_THRESHOLD) return

        val now = System.currentTimeMillis()
        if (now - lastShakeAt < SHAKE_COOLDOWN_MS) return
        lastShakeAt = now

        if (shakeToOpen) {
            ProwlUiLauncher.show(context)
        } else if (shakeToClear) {
            com.prowllabs.prowl.core.runtime.ProwlRuntime.storage.clearBlocking()
            com.prowllabs.prowl.core.runtime.ProwlRuntime.onLogsCleared()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    private const val SHAKE_THRESHOLD = 2.2
    private const val SHAKE_COOLDOWN_MS = 1_200L
}
