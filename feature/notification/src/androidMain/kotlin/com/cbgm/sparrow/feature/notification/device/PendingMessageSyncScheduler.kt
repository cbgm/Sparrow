package com.cbgm.sparrow.feature.notification.device

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.cbgm.sparrow.core.util.logging.SparrowLog

class PendingMessageSyncScheduler(
    private val context: Context
) {
    private val logger = SparrowLog.withTag("PendingMessageSyncScheduler")

    fun enqueue(wakeUpId: String) {
        require(wakeUpId.isNotBlank()) {
            "Wake-up ID must not be blank"
        }

        val request =
            OneTimeWorkRequestBuilder<PendingMessageSyncWorker>()
                .setInputData(
                    workDataOf(
                        PendingMessageSyncWorker.KEY_WAKE_UP_ID to wakeUpId
                    )
                ).setConstraints(
                    Constraints
                        .Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                ).setExpedited(
                    OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST
                ).build()

        /*
         * One device has one local routing identity, so every wake-up inbox exposes all pending envelopes.
         *
         * A sync that is running is never cancelled: it may already have stored and acknowledged messages and
         * would then never show their notifications. The new wake-up runs right after it instead. A sync that
         * only waits for a retry after an old node or mailbox failure is replaced, so a fresh wake-up never
         * waits behind it.
         */
        val workManager = WorkManager.getInstance(context)
        workManager.enqueueUniqueWork(WORK_NAME, policyFor(workManager), request)

        logger.info {
            "Pending-message sync scheduled; wakeUpId=${wakeUpId.take(LOG_WAKE_UP_ID_LENGTH)}"
        }
    }

    private fun policyFor(workManager: WorkManager): ExistingWorkPolicy {
        val running =
            runCatching { workManager.getWorkInfosForUniqueWork(WORK_NAME).get() }
                .getOrDefault(emptyList())
                .any { info -> info.state == WorkInfo.State.RUNNING }
        return if (running) ExistingWorkPolicy.APPEND_OR_REPLACE else ExistingWorkPolicy.REPLACE
    }

    private companion object {
        const val WORK_NAME = "sparrow-pending-message-sync"
        const val LOG_WAKE_UP_ID_LENGTH = 8
    }
}
