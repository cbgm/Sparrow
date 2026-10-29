package com.cbgm.sparrow.feature.notification.device

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.cbgm.sparrow.core.util.logging.SparrowLog
import com.cbgm.sparrow.feature.notification.domain.model.AppVisibilityState
import com.cbgm.sparrow.feature.notification.domain.usecase.SynchronizePendingMessagesUseCase
import com.cbgm.sparrow.feature.notification.presentation.ConversationNotificationPresenter
import com.cbgm.sparrow.feature.transport.domain.usecase.PrepareControlPlaneUseCase

class PendingMessageSyncWorker(
    appContext: Context,
    workerParameters: WorkerParameters,
    private val synchronizePendingMessages: SynchronizePendingMessagesUseCase,
    private val prepareControlPlane: PrepareControlPlaneUseCase,
    private val appVisibilityState: AppVisibilityState,
    private val conversationNotificationPresenter: ConversationNotificationPresenter,
    private val backgroundDeliveryReceiptSender: BackgroundDeliveryReceiptSender,
    private val notificationManager: SparrowNotificationManager
) : CoroutineWorker(appContext, workerParameters) {
    private val logger = SparrowLog.withTag("PendingMessageSyncWorker")

    /** Urgent work runs as a foreground job on Android 10 and 11, which fails without this notification. */
    override suspend fun getForegroundInfo(): ForegroundInfo =
        ForegroundInfo(SYNC_NOTIFICATION_ID, notificationManager.createSyncNotification())

    override suspend fun doWork(): Result {
        val wakeUpId =
            inputData.getString(KEY_WAKE_UP_ID)
                ?: return Result.failure()

        logger.info {
            "Pending-message sync started; " +
                "wakeUpId=${wakeUpId.take(LOG_WAKE_UP_ID_LENGTH)}, attempt=$runAttemptCount"
        }

        // A push can start Sparrow's process while the app is closed; the app's startup then never ran,
        // so the control planes (incl. those of the signed directory) are prepared here first.
        val syncResult =
            runCatching {
                prepareControlPlane().getOrThrow()
                synchronizePendingMessages(wakeUpId = wakeUpId).getOrThrow()
            }.getOrElse { error ->
                if (error is kotlinx.coroutines.CancellationException &&
                    error !is kotlinx.coroutines.TimeoutCancellationException
                ) {
                    throw error
                }
                logger.error(error) { "Pending-message sync failed; attempt=$runAttemptCount" }
                return if (runAttemptCount >= MAX_RETRY_COUNT) Result.failure() else Result.retry()
            }

        // The received message has already been persisted. Show its notification
        // now, rather than delaying it until the separate outgoing receipt is sent.
        if (!appVisibilityState.isVisible.value) {
            syncResult.notifications.forEach { notification ->
                conversationNotificationPresenter.show(notification)
            }
        }

        return runCatching {
            backgroundDeliveryReceiptSender.flush()
        }.fold(
            onSuccess = {
                logger.info {
                    "Pending-message sync succeeded; " +
                        "processed=${syncResult.processedEnvelopeCount}, " +
                        "notifications=${syncResult.notifications.size}; delivery receipts checked"
                }
                Result.success()
            },
            onFailure = { error ->
                if (error is kotlinx.coroutines.CancellationException &&
                    error !is kotlinx.coroutines.TimeoutCancellationException
                ) {
                    throw error
                }
                // Ordinary background unavailability must not produce a global
                // error popup. Receipts stay in ProtocolOutbox until accepted.
                logger.warn { "Delivery receipts remain queued for background retry: ${error.message}" }
                if (runAttemptCount >= MAX_RETRY_COUNT) Result.failure() else Result.retry()
            }
        )
    }

    companion object {
        const val KEY_WAKE_UP_ID = "wake-up-id"

        private const val MAX_RETRY_COUNT = 5
        private const val SYNC_NOTIFICATION_ID = 10_002
        private const val LOG_WAKE_UP_ID_LENGTH = 8
    }
}
