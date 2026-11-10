package com.cbgm.sparrow.feature.notification.device

import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import com.cbgm.sparrow.core.util.logging.SparrowLog
import com.cbgm.sparrow.feature.transport.background.ConnectionKeepAlive
import com.cbgm.sparrow.feature.transport.background.HeadlessStart
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

/**
 * Keeps Sparrow connected while the app is closed ("Always connected"): as long as it runs, the circuits, the mailbox
 * waits and the outbox keep running ([ConnectionKeepAlive]), so messages arrive at once without any push service.
 * Android only allows that with a visible notification. Started again by Android after it was stopped (sticky).
 */
class ConnectionService : Service() {
    private val logger = SparrowLog.withTag("ConnectionService")
    private val keepAlive by inject<ConnectionKeepAlive>()
    private val headlessStart by inject<HeadlessStart>()
    private val notificationManager by inject<SparrowNotificationManager>()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        startInForeground()
        keepAlive.hold()
        // Started without the app (after a reboot), the runtime is not running yet.
        scope.launch {
            runCatching { headlessStart.start() }.onFailure { error -> logger.warn { "Sparrow could not start in the background: ${error.message}" } }
        }
        logger.info { "Connection kept while Sparrow is closed" }
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {
        startInForeground()
        return START_STICKY
    }

    override fun onDestroy() {
        keepAlive.release()
        scope.cancel()
        logger.info { "Connection no longer kept while Sparrow is closed" }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startInForeground() {
        val notification = notificationManager.createConnectionNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(SparrowNotificationManager.CONNECTION_NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(SparrowNotificationManager.CONNECTION_NOTIFICATION_ID, notification)
        }
    }
}
