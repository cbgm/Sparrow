package com.cbgm.securechat.notification.push

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.cbgm.securechat.core.logging.SecureChatLog
import com.cbgm.securechat.notification.work.PendingMessageSyncWorker

class PendingMessageSyncScheduler(
    private val context: Context
) {
    private val logger = SecureChatLog.withTag("PendingMessageSyncScheduler")

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
         * One device has one local routing identity. Every wake-up inbox therefore
         * resolves to the same recipient and exposes all currently pending envelopes.
         *
         * REPLACE is intentional: a previous sync may be retrying because of an old
         * node/mailbox failure. A fresh high-priority FCM wake-up must not wait behind
         * that stale WorkManager chain.
         */
        WorkManager
            .getInstance(context)
            .enqueueUniqueWork(
                WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                request
            )

        logger.info {
            "Pending-message sync scheduled; wakeUpId=${wakeUpId.take(LOG_WAKE_UP_ID_LENGTH)}"
        }
    }

    private companion object {
        const val WORK_NAME = "securechat-pending-message-sync"
        const val LOG_WAKE_UP_ID_LENGTH = 8
    }
}
