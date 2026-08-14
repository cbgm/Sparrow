package com.cbgm.securechat.feature.chats.data.direct.outgoing

import com.cbgm.securechat.core.crypto.transport.TransportEncryptionMode
import com.cbgm.securechat.core.id.IdGenerator
import com.cbgm.securechat.core.logging.SecureChatLog
import com.cbgm.securechat.core.protocol.outbox.ProtocolOutbox
import com.cbgm.securechat.core.protocol.packet.ChatMessagePacket
import com.cbgm.securechat.core.protocol.packet.ReadReceiptPacket
import com.cbgm.securechat.core.protocol.phone.LocalPhoneNumberProvider
import com.cbgm.securechat.core.time.SystemClock
import com.cbgm.securechat.data.database.dao.ChatDao
import com.cbgm.securechat.data.database.entity.MessageEntity
import com.cbgm.securechat.feature.chats.data.direct.delivery.DirectMessageDeliveryCoordinator
import com.cbgm.securechat.feature.chats.data.direct.mapper.toDirectDeliveryStatus
import com.cbgm.securechat.feature.chats.domain.model.MessageContentStatus
import com.cbgm.securechat.feature.chats.domain.model.MessageDeliveryEvent
import com.cbgm.securechat.feature.chats.domain.model.MessageDeliveryStatus
import com.cbgm.securechat.feature.chats.domain.model.direct.DirectMessageDeliveryStateMachine
import com.cbgm.securechat.feature.contacts.domain.model.Contact
import com.cbgm.securechat.feature.contacts.domain.model.KeyExchangeStatus
import com.cbgm.securechat.feature.contacts.domain.repository.IdentityInvitationRepository
import com.cbgm.securechat.feature.contacts.domain.usecase.GetContactUseCase

/**
 * Owns every outgoing direct-message operation.
 *
 * Red line:
 * Direct use case -> DirectConversationRepositoryImpl -> this processor -> ProtocolOutbox.
 */
class DirectOutgoingMessageProcessor(
    private val chatDao: ChatDao,
    private val getContact: GetContactUseCase,
    private val localPhoneNumberProvider: LocalPhoneNumberProvider,
    private val protocolOutbox: ProtocolOutbox,
    private val identityInvitationRepository: IdentityInvitationRepository,
    private val deliveryCoordinator: DirectMessageDeliveryCoordinator
) {
    private val logger = SecureChatLog.withTag("DirectOutgoingMessageProcessor")

    suspend fun send(
        conversationId: String,
        text: String
    ): Result<Unit> =
        runCatching {
            val normalizedText = text.trim()
            require(normalizedText.isNotEmpty()) { "Message text must not be blank" }

            val target = loadTarget(conversationId)
            identityInvitationRepository
                .requireDirectChatAuthorization(target.contactId)
                .getOrThrow()

            val contact = getContact(target.contactId).getOrThrow() ?: error("Contact was not found")
            val packet = createPacket(normalizedText)
            storeQueuedMessage(target, normalizedText, packet, contact)
            enqueue(target.contactId, packet)
        }

    suspend fun retry(messageId: String): Result<Unit> =
        runCatching {
            require(messageId.isNotBlank()) { "Message ID must not be blank" }

            val message = chatDao.findMessageById(messageId) ?: error("Message was not found")
            check(message.isMine) { "Only outgoing messages can be retried" }
            check(
                DirectMessageDeliveryStateMachine.canTransition(
                    current = message.deliveryStatus.toDirectDeliveryStatus(),
                    event = MessageDeliveryEvent.RETRY_REQUESTED
                )
            ) {
                "Only failed direct messages can be retried"
            }

            val target = loadTarget(message.conversationId)
            identityInvitationRepository
                .requireDirectChatAuthorization(target.contactId)
                .getOrThrow()

            val packetId = message.packetId?.takeIf(String::isNotBlank) ?: error("Message has no linked protocol packet")
            val outboxItem = protocolOutbox.findByPacketId(packetId).getOrThrow()
                ?: error("Linked outbox item was not found")

            protocolOutbox.retry(outboxItem.id).getOrThrow()
            deliveryCoordinator.applyRetryEvent(messageId)
        }

    suspend fun sendReadReceipts(conversationId: String): Result<Unit> =
        runCatching {
            require(conversationId.isNotBlank()) { "Conversation ID must not be blank" }
            loadTarget(conversationId)

            chatDao.findMessagesAwaitingReadReceipt(conversationId).forEach { message ->
                enqueueReadReceipt(message.messageId, message.contactId)
                check(chatDao.markReadReceiptSent(message.messageId) == 1) {
                    "Incoming direct message could not be marked as read"
                }
                logger.debug {
                    "Direct read receipt queued: messageId=${message.messageId}, contactId=${message.contactId}"
                }
            }
        }

    private suspend fun loadTarget(conversationId: String): DirectTarget {
        val conversation = chatDao.findConversationById(conversationId)
            ?: error("Direct conversation was not found")
        check(conversation.type == DIRECT_CONVERSATION_TYPE) { "Conversation is not direct" }
        val contactId = requireNotNull(conversation.contactId) { "Direct conversation has no contact" }
        return DirectTarget(conversationId = conversation.id, contactId = contactId)
    }

    private suspend fun createPacket(text: String): ChatMessagePacket =
        ChatMessagePacket(
            packetId = IdGenerator.generate(prefix = "packet"),
            messageId = IdGenerator.generate(prefix = "message"),
            sentAtEpochMilliseconds = SystemClock.nowEpochMilliseconds(),
            text = text,
            senderPhoneNumber = localPhoneNumberProvider.getLocalPhoneNumber().getOrThrow()
        )

    private suspend fun storeQueuedMessage(
        target: DirectTarget,
        text: String,
        packet: ChatMessagePacket,
        contact: Contact
    ) {
        chatDao.upsertMessage(
            MessageEntity(
                id = packet.messageId,
                conversationId = target.conversationId,
                packetId = packet.packetId,
                text = text,
                transportPayload = null,
                transportMode = contact.plannedTransportMode().name,
                contentStatus = MessageContentStatus.READABLE.name,
                deliveryStatus = MessageDeliveryStatus.QUEUED.name,
                senderContactId = null,
                isMine = true,
                createdAtEpochMilliseconds = packet.sentAtEpochMilliseconds
            )
        )
        chatDao.updateConversationTimestamp(target.conversationId, packet.sentAtEpochMilliseconds)
    }

    private suspend fun enqueue(
        contactId: String,
        packet: ChatMessagePacket
    ) {
        protocolOutbox.enqueue(contactId, packet).getOrElse { error ->
            deliveryCoordinator.applyPacketEvent(
                packetId = packet.packetId,
                event = MessageDeliveryEvent.SEND_FAILED,
                errorMessage = error.message
            )
            throw error
        }
    }

    private suspend fun enqueueReadReceipt(
        messageId: String,
        contactId: String
    ) {
        protocolOutbox.enqueue(
            contactId = contactId,
            packet =
                ReadReceiptPacket(
                    packetId = "read-receipt-$messageId",
                    messageId = messageId,
                    readAtEpochMilliseconds = SystemClock.nowEpochMilliseconds()
                )
        ).getOrThrow()
    }

    private fun Contact.plannedTransportMode(): TransportEncryptionMode {
        val identity = secureChatIdentity
        val canEncrypt =
            identity != null &&
                identity.encryptionPublicKey.isNotEmpty() &&
                identity.keyExchangeStatus == KeyExchangeStatus.MUTUAL
        return if (canEncrypt) TransportEncryptionMode.SEALED_BOX else TransportEncryptionMode.PLAINTEXT
    }

    private data class DirectTarget(
        val conversationId: String,
        val contactId: String
    )

    private companion object {
        const val DIRECT_CONVERSATION_TYPE = "DIRECT"
    }
}
