package com.cbgm.sparrow.notification.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.cbgm.sparrow.core.logging.SparrowLog
import com.cbgm.sparrow.notification.application.AppVisibilityState
import com.cbgm.sparrow.notification.application.SynchronizePendingMessages
import com.cbgm.sparrow.notification.presentation.ConversationNotificationPresenter

class PendingMessageSyncWorker(
    appContext: Context,
    workerParameters: WorkerParameters,
    private val synchronizePendingMessages: SynchronizePendingMessages,
    private val appVisibilityState: AppVisibilityState,
    private val conversationNotificationPresenter: ConversationNotificationPresenter
) : CoroutineWorker(appContext, workerParameters) {
    private val logger = SparrowLog.withTag("PendingMessageSyncWorker")

    override suspend fun doWork(): Result {
        val wakeUpId =
            inputData.getString(KEY_WAKE_UP_ID)
                ?: return Result.failure()

        logger.info {
            "Pending-message sync started; " +
                "wakeUpId=${wakeUpId.take(LOG_WAKE_UP_ID_LENGTH)}, attempt=$runAttemptCount"
        }

        return synchronizePendingMessages(wakeUpId = wakeUpId).fold(
            onSuccess = { syncResult ->
                if (!appVisibilityState.isVisible.value) {
                    syncResult.notifications.forEach { notification ->
                        conversationNotificationPresenter.show(notification)
                    }
                }

                logger.info {
                    "Pending-message sync succeeded; " +
                        "processed=${syncResult.processedEnvelopeCount}, " +
                        "notifications=${syncResult.notifications.size}"
                }

                Result.success()
            },
            onFailure = { error ->
                logger.error(error) {
                    "Pending-message sync failed; attempt=$runAttemptCount"
                }

                if (runAttemptCount >= MAX_RETRY_COUNT) {
                    Result.failure()
                } else {
                    Result.retry()
                }
            }
        )
    }

    companion object {
        const val KEY_WAKE_UP_ID = "wake-up-id"

        private const val MAX_RETRY_COUNT = 5
        private const val LOG_WAKE_UP_ID_LENGTH = 8
    }
}
