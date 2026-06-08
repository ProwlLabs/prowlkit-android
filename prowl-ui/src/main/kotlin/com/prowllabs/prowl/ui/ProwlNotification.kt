package com.prowllabs.prowl.ui

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.prowllabs.prowl.core.runtime.ProwlRuntime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/** Chucker-style persistent notification that opens the inspector. */
object ProwlNotification {
    private const val CHANNEL_ID = "prowl_inspector"
    private const val NOTIFICATION_ID = 0x70726F77 // "prow"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var observing = false

    fun show(context: Context) {
        createChannel(context)
        update(context, 0)
        if (!observing) {
            observing = true
            scope.launch {
                ProwlRuntime.storage.logsFlow.collectLatest { logs ->
                    update(context.applicationContext, logs.size)
                }
            }
        }
    }

    fun dismiss(context: Context) {
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
        observing = false
    }

    private fun update(context: Context, count: Int) {
        val launchIntent = ProwlUiLauncher.createIntent(context)
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_search)
            .setContentTitle("Prowl network inspector")
            .setContentText("$count captured requests")
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }

    private fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Prowl Inspector",
            NotificationManager.IMPORTANCE_LOW,
        )
        val manager = context.getSystemService(NotificationManager::class.java)
        manager?.createNotificationChannel(channel)
    }
}
