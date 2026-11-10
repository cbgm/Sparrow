package com.cbgm.sparrow.feature.notification.device

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.cbgm.sparrow.core.util.logging.SparrowLog
import com.cbgm.sparrow.feature.transport.background.ConnectionMode
import java.util.concurrent.TimeUnit

/** Turns the chosen [ConnectionMode] into what Android runs: the connection service, a periodic check, or nothing. */
class ConnectionModeApplier(
    private val context: Context
) {
    private val logger = SparrowLog.withTag("ConnectionModeApplier")

    fun apply(mode: ConnectionMode) {
        when (mode) {
            ConnectionMode.ALWAYS_CONNECTED -> {
                cancelChecks()
                startService()
            }
            ConnectionMode.WHILE_OPEN -> {
                cancelChecks()
                stopService()
            }
            ConnectionMode.CHECK_PERIODICALLY -> {
                stopService()
                scheduleChecks()
            }
        }
    }

    /** Android refuses a foreground service started while the app is in the background, except after a reboot. */
    private fun startService() {
        runCatching { ContextCompat.startForegroundService(context, Intent(context, ConnectionService::class.java)) }
            .onFailure { error -> logger.warn { "Connection service not started: ${error.message}" } }
    }

    private fun stopService() {
        context.stopService(Intent(context, ConnectionService::class.java))
    }

    private fun scheduleChecks() {
        val request = PeriodicWorkRequestBuilder<ConnectionCheckWorker>(CHECK_INTERVAL_MINUTES, TimeUnit.MINUTES).build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(CHECK_WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
    }

    private fun cancelChecks() {
        WorkManager.getInstance(context).cancelUniqueWork(CHECK_WORK_NAME)
    }

    private companion object {
        const val CHECK_WORK_NAME = "sparrow-connection-check"
        const val CHECK_INTERVAL_MINUTES = 15L
    }
}
