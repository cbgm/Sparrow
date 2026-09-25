package com.cbgm.sparrow.notification.device

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.cbgm.sparrow.core.logging.SparrowLog
import com.cbgm.sparrow.core.transport.ControlPlaneConfiguration
import com.cbgm.sparrow.notification.domain.model.AppVisibilityState
import com.cbgm.sparrow.notification.domain.usecase.SynchronizePendingMessagesUseCase
import com.cbgm.sparrow.notification.presentation.ConversationNotificationPresenter

class PendingMessageSyncWorker(
    appContext: Context,
    workerParameters: WorkerParameters,
    private val synchronizePendingMessages: SynchronizePendingMessagesUseCase,
    private val controlPlaneConfiguration: ControlPlaneConfiguration,
    private val appVisibilityState: AppVisibilityState,
    private val conversationNotificationPresenter: ConversationNotificationPresenter,
    private val backgroundDeliveryReceiptSender: BackgroundDeliveryReceiptSender
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

        // WorkManager can start Sparrow's process without ever creating AppViewModel.
        // The normal UI startup initializes the persisted control-plane endpoints,
        // but a cold push must do that independently before making network requests.
        val syncResult =
            runCatching {
                controlPlaneConfiguration.initialize()
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
        private const val LOG_WAKE_UP_ID_LENGTH = 8
    }
}
