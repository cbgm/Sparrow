package com.cbgm.sparrow.feature.notification.device

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.cbgm.sparrow.core.util.logging.SparrowLog
import com.cbgm.sparrow.feature.transport.domain.port.ConnectionKeepAlivePort
import com.cbgm.sparrow.feature.transport.domain.port.HeadlessStartPort
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

/**
 * "Check now and then": Android runs this every 15 minutes at most (less in deep sleep). It connects for a short
 * while, long enough to build the circuits and fetch what waits in the mailboxes, then lets go.
 */
class ConnectionCheckWorker(
    appContext: Context,
    workerParameters: WorkerParameters,
    private val keepAlive: ConnectionKeepAlivePort,
    private val headlessStart: HeadlessStartPort,
    private val checkMilliseconds: Long = CHECK_MILLISECONDS
) : CoroutineWorker(appContext, workerParameters) {
    private val logger = SparrowLog.withTag("ConnectionCheckWorker")

    override suspend fun doWork(): Result {
        keepAlive.hold()
        return try {
            headlessStart.start()
            delay(checkMilliseconds.milliseconds)
            logger.info { "Checked for messages while Sparrow is closed" }
            Result.success()
        } finally {
            keepAlive.release()
        }
    }

    private companion object {
        const val CHECK_MILLISECONDS = 45_000L
    }
}
