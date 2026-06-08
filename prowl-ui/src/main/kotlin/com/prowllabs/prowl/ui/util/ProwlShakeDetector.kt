package com.prowllabs.prowl.ui.util

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import kotlin.math.sqrt

@Composable
fun ProwlShakeDetector(
    enabled: Boolean,
    onShake: () -> Unit,
) {
    val context = LocalContext.current
    val callback = remember(onShake) { onShake }

    DisposableEffect(enabled, context) {
        if (!enabled) {
            return@DisposableEffect onDispose {}
        }
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        if (accelerometer == null) {
            return@DisposableEffect onDispose {}
        }

        val detector = ShakeEventListener(onShake = callback)
        sensorManager.registerListener(
            detector,
            accelerometer,
            SensorManager.SENSOR_DELAY_UI,
        )
        onDispose {
            sensorManager.unregisterListener(detector)
        }
    }
}

private class ShakeEventListener(
    private val onShake: () -> Unit,
) : SensorEventListener {
    private var lastShakeAt = 0L

    override fun onSensorChanged(event: SensorEvent) {
        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]
        val gForce = sqrt((x * x + y * y + z * z).toDouble()) / SensorManager.GRAVITY_EARTH
        if (gForce > SHAKE_THRESHOLD) {
            val now = System.currentTimeMillis()
            if (now - lastShakeAt > SHAKE_COOLDOWN_MS) {
                lastShakeAt = now
                onShake()
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    companion object {
        private const val SHAKE_THRESHOLD = 2.2
        private const val SHAKE_COOLDOWN_MS = 1_200L
    }
}
