package com.cbgm.securechat.notification.application

import com.cbgm.securechat.core.crypto.InitializeCryptoRuntime
import com.cbgm.securechat.core.logging.SecureChatLog
import com.cbgm.securechat.feature.chats.domain.model.overview.ConversationOverview
import com.cbgm.securechat.feature.chats.domain.usecase.overview.ObserveConversationOverviewsUseCase
import com.cbgm.securechat.feature.messaging.application.incoming.IncomingEnvelopeProcessingResult
import com.cbgm.securechat.feature.messaging.application.incoming.IncomingEnvelopeProcessor
import com.cbgm.securechat.feature.messaging.application.mailbox.MailboxCoordinator
import com.cbgm.securechat.feature.transport.relay.inbox.PendingRelayEnvelopeGateway
import com.cbgm.securechat.notification.model.ConversationNotification
import com.cbgm.securechat.notification.model.PendingMessageSyncResult
import kotlinx.coroutines.flow.first

class SynchronizePendingMessages(
    private val initializeCryptoRuntime: InitializeCryptoRuntime,
    private val pendingRelayEnvelopeGateway: PendingRelayEnvelopeGateway,
    private val incomingEnvelopeProcessor: IncomingEnvelopeProcessor,
    private val observeConversations: ObserveConversationOverviewsUseCase,
    private val mailboxCoordinator: MailboxCoordinator
) {
    private val logger = SecureChatLog.withTag("SynchronizePendingMessages")

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
        observeConversations()
            .first()
            .associate { conversation ->
                conversation.id to conversation.unreadCount
            }

    private suspend fun processPushEnvelopes(wakeUpId: String): Int {
        val envelopes =
            pendingRelayEnvelopeGateway
                .getPendingEnvelopes(wakeUpId = wakeUpId)
                .getOrThrow()
        var processedEnvelopeCount = 0

        envelopes.forEach { envelope ->
            val result =
                incomingEnvelopeProcessor
                    .process(
                        envelopeId = envelope.envelopeId,
                        senderRelayId = envelope.senderId,
                        encodedTransportPayload = envelope.payload
                    ).getOrThrow()

            if (result == IncomingEnvelopeProcessingResult.Processed) {
                pendingRelayEnvelopeGateway
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
        observeConversations()
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
