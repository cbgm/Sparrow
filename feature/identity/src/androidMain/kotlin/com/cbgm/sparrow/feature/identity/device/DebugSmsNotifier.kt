package com.cbgm.sparrow.feature.identity.device

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build

/** Debug builds in Firebase test mode: shows the test code as an SMS-like notification. */
class DebugSmsNotifier(
    private val context: Context
) {
    fun show(code: String) {
        val allowed =
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
        if (!allowed) return
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH)
        )
        val notification =
            Notification
                .Builder(context, CHANNEL_ID)
                .setSmallIcon(context.applicationInfo.icon)
                .setContentTitle(SENDER)
                .setContentText("$code is your Sparrow verification code.")
                .setAutoCancel(true)
                .build()
        manager.notify(NOTIFICATION_ID, notification)
    }

    private companion object {
        const val CHANNEL_ID = "sparrow-debug-sms"
        const val CHANNEL_NAME = "Debug SMS"
        const val SENDER = "Sparrow (test SMS)"
        const val NOTIFICATION_ID = 4711
    }
}
