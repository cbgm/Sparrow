package com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.usecase

import com.cbgm.sparrow.core.base.message.domain.model.MessageContentStatus
import com.cbgm.sparrow.core.base.message.domain.model.MessageDeliveryStatus
import com.cbgm.sparrow.core.crypto.hash.CryptoHash
import com.cbgm.sparrow.core.util.extensions.toHexString
import com.cbgm.sparrow.core.util.logging.SparrowLog
import com.cbgm.sparrow.core.util.result.safeSuspendCall
import com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.model.DirectPeerAccess
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.model.StoredMessage
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.repository.DirectConversationRepository
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.repository.DirectMessageRepository

/**
 * Keeps a message that could not be read, so the user sees that something arrived. Like readable messages
 * (see [ResolveDirectPeerAccessUseCase]), only an authorized contact gets the placeholder, and only in its
 * existing direct chat: an unreadable envelope never creates a chat, for example for a group-only member.
 * Envelopes of pending, unknown or blocked contacts are dropped; they cannot be replayed once the exchange
 * completes, because the key that failed would fail again. The id is derived from the envelope, so a
 * redelivered envelope does not add a second placeholder.
 */
class StoreUnreadableMessageUseCase(
    private val resolveDirectPeerAccess: ResolveDirectPeerAccessUseCase,
    private val conversationRepository: DirectConversationRepository,
    private val messageRepository: DirectMessageRepository,
    private val cryptoHash: CryptoHash
) {
    private val logger = SparrowLog.withTag("StoreUnreadableMessageUseCase")

    /** Returns whether a placeholder is stored (false when it was dropped or already exists). */
    @Suppress("LongParameterList")
    suspend operator fun invoke(
        contactId: String,
        encodedTransportPayload: String,
        text: String,
        transportMode: String,
        contentStatus: MessageContentStatus,
        receivedAtEpochMilliseconds: Long
    ): Result<Boolean> =
        safeSuspendCall(logFailure = false) {
            val conversationId = findAcceptingConversationId(contactId, contentStatus) ?: return@safeSuspendCall false
            val messageId = placeholderId(contactId, encodedTransportPayload)
            if (messageRepository.find(messageId).getOrThrow() != null) return@safeSuspendCall false
            messageRepository.saveIncoming(
                message =
                    StoredMessage(
                        id = messageId,
                        conversationId = conversationId,
                        packetId = null,
                        replyToMessageId = null,
                        transportPayload = encodedTransportPayload,
                        transportMode = transportMode,
                        contentStatus = contentStatus,
                        deliveryStatus = MessageDeliveryStatus.NOT_APPLICABLE,
                        senderContactId = contactId,
                        readReceiptSent = false,
                        isMine = false,
                        createdAtEpochMilliseconds = receivedAtEpochMilliseconds
                    ),
                text = text,
                receivedAtEpochMilliseconds = receivedAtEpochMilliseconds
            ).getOrThrow()
            true
        }

    private suspend fun findAcceptingConversationId(contactId: String, contentStatus: MessageContentStatus): String? {
        val access = resolveDirectPeerAccess(contactId).getOrThrow()
        if (access != DirectPeerAccess.AUTHORIZED) {
            logger.warn { "Dropped unreadable envelope ($contentStatus) of $access contact $contactId" }
            return null
        }
        val conversationId = conversationRepository.findConversationId(contactId).getOrThrow()
        if (conversationId == null) {
            logger.warn { "Dropped unreadable envelope ($contentStatus) of $contactId without a direct chat" }
        }
        return conversationId
    }

    private fun placeholderId(contactId: String, encodedTransportPayload: String): String {
        val digest = cryptoHash.sha256("$contactId\n$encodedTransportPayload".encodeToByteArray())
        return "$ID_PREFIX-${digest.toHexString()}"
    }

    private companion object {
        const val ID_PREFIX = "failed-message"
    }
}
