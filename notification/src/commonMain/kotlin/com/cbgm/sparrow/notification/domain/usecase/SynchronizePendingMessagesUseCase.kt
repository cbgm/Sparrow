package com.cbgm.sparrow.notification.domain.usecase

import com.cbgm.sparrow.core.crypto.InitializeCryptoRuntime
import com.cbgm.sparrow.core.logging.SparrowLog
import com.cbgm.sparrow.feature.chats.domain.model.overview.ConversationOverview
import com.cbgm.sparrow.feature.chats.domain.repository.overview.ConversationOverviewRepository
import com.cbgm.sparrow.feature.messaging.application.incoming.IncomingEnvelopeProcessingResult
import com.cbgm.sparrow.feature.messaging.application.incoming.IncomingEnvelopeProcessor
import com.cbgm.sparrow.feature.messaging.application.mailbox.MailboxCoordinator
import com.cbgm.sparrow.feature.transport.push.inbox.PendingEnvelopeGateway
import com.cbgm.sparrow.notification.domain.model.ConversationNotification
import com.cbgm.sparrow.notification.domain.model.PendingMessageSyncResult
import kotlinx.coroutines.flow.first

class SynchronizePendingMessagesUseCase(
    private val initializeCryptoRuntime: InitializeCryptoRuntime,
    private val pendingEnvelopeGateway: PendingEnvelopeGateway,
    private val incomingEnvelopeProcessor: IncomingEnvelopeProcessor,
    private val conversationOverviewRepository: ConversationOverviewRepository,
    private val mailboxCoordinator: MailboxCoordinator
) {
    private val logger = SparrowLog.withTag("SynchronizePendingMessagesUseCase")

    suspend operator fun invoke(wakeUpId: String): Result<PendingMessageSyncResult> =
        runCatching {
            initializeCryptoRuntime().getOrThrow()
            require(wakeUpId.isNotBlank()) {
                "Wake-up ID must not be blank"
            }

            val unreadCountsBeforeSync = loadUnreadCounts()
            val pushProcessed = processPushEnvelopes(wakeUpId = wakeUpId)
            val mailboxProcessed = synchronizeMailbox()
            val processedEnvelopeCount = pushProcessed + mailboxProcessed
            val notifications = buildNotifications(unreadCountsBeforeSync)

            logger.info {
                "Push synchronization completed; " +
                    "wakeUpId=${wakeUpId.take(LOG_WAKE_UP_ID_LENGTH)}, " +
                    "processed=$processedEnvelopeCount, notifications=${notifications.size}"
            }

            PendingMessageSyncResult(
                processedEnvelopeCount = processedEnvelopeCount,
                notifications = notifications
            )
        }

    private suspend fun loadUnreadCounts(): Map<String, Int> =
        conversationOverviewRepository
            .observeAll()
            .first()
            .associate { conversation ->
                conversation.id to conversation.unreadCount
            }

    private suspend fun processPushEnvelopes(wakeUpId: String): Int {
        val envelopes =
            pendingEnvelopeGateway
                .getPendingEnvelopes(wakeUpId = wakeUpId)
                .getOrThrow()
        var processedEnvelopeCount = 0

        envelopes.forEach { envelope ->
            val result =
                incomingEnvelopeProcessor
                    .process(
                        envelopeId = envelope.envelopeId,
                        senderRoutingId = envelope.senderId,
                        encodedTransportPayload = envelope.payload
                    ).getOrThrow()

            if (result == IncomingEnvelopeProcessingResult.Processed) {
                pendingEnvelopeGateway
                    .acknowledge(
                        wakeUpId = wakeUpId,
                        envelopeId = envelope.envelopeId
                    ).getOrThrow()
                processedEnvelopeCount += 1
            }
        }

        return processedEnvelopeCount
    }

    private suspend fun synchronizeMailbox(): Int =
        mailboxCoordinator
            .synchronizePending()
            .getOrElse { error ->
                logger.warn(error) {
                    "Mailbox synchronization failed during push wake-up; " +
                        "central push synchronization already completed"
                }
                0
            }

    private suspend fun buildNotifications(
        unreadCountsBeforeSync: Map<String, Int>
    ): List<ConversationNotification> =
        conversationOverviewRepository
            .observeAll()
            .first()
            .mapNotNull { conversation ->
                val unreadCountBeforeSync = unreadCountsBeforeSync[conversation.id] ?: 0
                conversation
                    .takeIf { current ->
                        current.unreadCount > unreadCountBeforeSync
                    }?.toNotification()
            }

    private fun ConversationOverview.toNotification(): ConversationNotification =
        ConversationNotification(
            conversationId = id,
            title = displayName,
            messagePreview = lastMessageText?.takeIf(String::isNotBlank),
            unreadCount = unreadCount
        )

    private companion object {
        const val LOG_WAKE_UP_ID_LENGTH = 8
    }
}
