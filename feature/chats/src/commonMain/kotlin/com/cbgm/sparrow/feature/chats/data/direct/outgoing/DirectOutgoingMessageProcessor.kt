package com.cbgm.sparrow.feature.chats.data.direct.outgoing

import com.cbgm.sparrow.core.crypto.transport.TransportEncryptionMode
import com.cbgm.sparrow.core.id.IdGenerator
import com.cbgm.sparrow.core.logging.SparrowLog
import com.cbgm.sparrow.core.protocol.attachment.MessageAttachment
import com.cbgm.sparrow.core.protocol.message.MessageDeletionPayload
import com.cbgm.sparrow.core.protocol.message.MessageReactionPayload
import com.cbgm.sparrow.core.protocol.outbox.ProtocolOutbox
import com.cbgm.sparrow.core.protocol.packet.ChatMessagePacket
import com.cbgm.sparrow.core.protocol.packet.ReadReceiptPacket
import com.cbgm.sparrow.core.protocol.phone.LocalPhoneNumberProvider
import com.cbgm.sparrow.core.protocol.profile.LocalProfilePictureMetadataProvider
import com.cbgm.sparrow.core.protocol.profile.ProfilePictureMetadata
import com.cbgm.sparrow.core.time.SystemClock
import com.cbgm.sparrow.data.database.dao.ChatDao
import com.cbgm.sparrow.data.database.dao.MessageReactionDao
import com.cbgm.sparrow.data.database.entity.MessageEntity
import com.cbgm.sparrow.data.database.entity.MessageReactionEntity
import com.cbgm.sparrow.feature.attachments.data.datasource.MessageAttachmentDataSource
import com.cbgm.sparrow.feature.attachments.data.model.PreparedMessageAttachmentDto
import com.cbgm.sparrow.feature.attachments.domain.model.MessageAttachmentPolicy
import com.cbgm.sparrow.feature.attachments.domain.model.OutgoingMessageAttachment
import com.cbgm.sparrow.feature.chats.data.direct.delivery.DirectMessageDeliveryCoordinator
import com.cbgm.sparrow.feature.chats.data.direct.mapper.toMessageDeliveryStatus
import com.cbgm.sparrow.feature.chats.domain.model.MessageContentStatus
import com.cbgm.sparrow.feature.chats.domain.model.MessageDeliveryEvent
import com.cbgm.sparrow.feature.chats.domain.model.MessageDeliveryStatus
import com.cbgm.sparrow.feature.chats.domain.model.direct.DirectMessageDeliveryStateMachine
import com.cbgm.sparrow.feature.chats.domain.model.direct.DirectPendingAuthorizationMessagePolicy
import com.cbgm.sparrow.feature.contacts.domain.model.Contact
import com.cbgm.sparrow.feature.contacts.domain.model.KeyExchangeStatus
import com.cbgm.sparrow.feature.contacts.domain.usecase.GetContactUseCase
import com.cbgm.sparrow.feature.contacts.domain.usecase.RequireDirectChatAuthorizationUseCase

/**
 * Owns every outgoing direct-message operation.
 *
 * Red line:
 * Direct use case -> DirectMessageRepositoryImpl -> this processor -> ProtocolOutbox.
 */
class DirectOutgoingMessageProcessor(
    private val chatDao: ChatDao,
    private val messageReactionDao: MessageReactionDao,
    private val getContact: GetContactUseCase,
    private val localPhoneNumberProvider: LocalPhoneNumberProvider,
    private val protocolOutbox: ProtocolOutbox,
    private val requireDirectChatAuthorization: RequireDirectChatAuthorizationUseCase,
    private val localProfilePictureMetadataProvider: LocalProfilePictureMetadataProvider,
    private val deliveryCoordinator: DirectMessageDeliveryCoordinator,
    private val attachmentTransfer: MessageAttachmentDataSource
) {
    private val logger = SparrowLog.withTag("DirectOutgoingMessageProcessor")

    suspend fun send(
        conversationId: String,
        text: String,
        attachments: List<OutgoingMessageAttachment> = emptyList(),
        replyToMessageId: String? = null
    ): Result<Unit> =
        runCatching {
            val normalizedText = requireMessageContent(text, attachments)
            val target = loadTarget(conversationId)
            requireDirectChatAuthorization(target.contactId).getOrThrow()

            val contact = getContact(target.contactId).getOrThrow() ?: error("Contact was not found")
            val messageId = IdGenerator.generate(prefix = "message")
            val prepared = attachmentTransfer.prepareAttachments(attachments).getOrThrow()
            persistPreparedMessage(
                target = target,
                contact = contact,
                messageId = messageId,
                text = normalizedText,
                prepared = prepared,
                deliveryStatus = MessageDeliveryStatus.QUEUED,
                replyToMessageId = replyToMessageId
            )
            val packet =
                try {
                    createPacket(
                        messageId = messageId,
                        text = normalizedText,
                        attachments = prepared.map(PreparedMessageAttachmentDto::attachment),
                        replyToMessageId = replyToMessageId
                    ).also { packet ->
                        linkPacket(messageId = messageId, packet = packet, contact = contact)
                    }
                } catch (error: Throwable) {
                    chatDao.findMessageById(messageId)?.let { storedMessage ->
                        discardMessages(listOf(storedMessage))
                    }
                    throw error
                }
            enqueue(target.contactId, packet)
        }

    suspend fun toggleReaction(conversationId: String, messageId: String, emoji: String): Result<Unit> =
        runCatching {
            require(messageId.isNotBlank()) { "Message ID must not be blank" }
            require(emoji.isNotBlank()) { "Reaction emoji must not be blank" }
            val target = loadTarget(conversationId)
            requireDirectChatAuthorization(target.contactId).getOrThrow()
            val message = chatDao.findMessageById(messageId) ?: error("Message was not found")
            check(message.conversationId == conversationId) { "Message does not belong to this conversation" }

            val existing = messageReactionDao.find(messageId, MessageReactionEntity.LOCAL_REACTOR_ID, emoji)
            val removed = existing != null
            if (removed) {
                messageReactionDao.delete(messageId, MessageReactionEntity.LOCAL_REACTOR_ID, emoji)
            } else {
                messageReactionDao.upsert(
                    MessageReactionEntity(messageId, conversationId, MessageReactionEntity.LOCAL_REACTOR_ID, emoji)
                )
            }

            val packet = ChatMessagePacket(
                packetId = IdGenerator.generate(prefix = "reaction-packet"),
                messageId = IdGenerator.generate(prefix = "reaction"),
                sentAtEpochMilliseconds = SystemClock.nowEpochMilliseconds(),
                text = "",
                reaction = MessageReactionPayload(messageId = messageId, emoji = emoji, removed = removed),
                senderPhoneNumber = localPhoneNumberProvider.getLocalPhoneNumber().getOrThrow(),
                profilePicture = localProfilePictureMetadataProvider.forMessage().getOrElse { ProfilePictureMetadata() }
            )
            protocolOutbox.enqueue(target.contactId, packet).getOrThrow()
        }

    suspend fun deleteMessage(conversationId: String, messageId: String): Result<Unit> =
        runCatching {
            require(messageId.isNotBlank()) { "Message ID must not be blank" }
            val target = loadTarget(conversationId)
            val message = chatDao.findMessageById(messageId) ?: error("Message was not found")
            check(message.conversationId == conversationId) { "Message does not belong to this conversation" }
            check(message.isMine) { "Only your own messages can be deleted for everyone" }

            if (message.deliveryStatus == MessageDeliveryStatus.WAITING_FOR_AUTHORIZATION.name) {
                discardMessages(listOf(message))
                return@runCatching
            }

            requireDirectChatAuthorization(target.contactId).getOrThrow()

            val packet =
                ChatMessagePacket(
                    packetId = IdGenerator.generate(prefix = "delete-packet"),
                    messageId = IdGenerator.generate(prefix = "delete"),
                    sentAtEpochMilliseconds = SystemClock.nowEpochMilliseconds(),
                    text = "",
                    deletion = MessageDeletionPayload(messageId),
                    senderPhoneNumber = localPhoneNumberProvider.getLocalPhoneNumber().getOrThrow(),
                    profilePicture =
                        localProfilePictureMetadataProvider
                            .forMessage()
                            .getOrElse { ProfilePictureMetadata() }
                )
            protocolOutbox.enqueue(target.contactId, packet).getOrThrow()
            discardMessages(listOf(message))
        }

    suspend fun queueUntilAuthorized(
        conversationId: String,
        text: String,
        attachments: List<OutgoingMessageAttachment> = emptyList(),
        replyToMessageId: String? = null
    ): Result<Unit> =
        runCatching {
            val normalizedText = requireMessageContent(text, attachments)
            val target = loadTarget(conversationId)
            val contact = getContact(target.contactId).getOrThrow() ?: error("Contact was not found")
            val prepared = attachmentTransfer.prepareAttachments(attachments).getOrThrow()
            persistPreparedMessage(
                target = target,
                contact = contact,
                messageId = IdGenerator.generate(prefix = "message"),
                text = normalizedText,
                prepared = prepared,
                deliveryStatus = MessageDeliveryStatus.WAITING_FOR_AUTHORIZATION,
                replyToMessageId = replyToMessageId
            )
        }

    suspend fun releaseWaitingForAuthorization(contactId: String): Result<Unit> =
        runCatching {
            requireDirectChatAuthorization(contactId).getOrThrow()
            val contact = getContact(contactId).getOrThrow() ?: error("Contact was not found")
            val nowEpochMilliseconds = SystemClock.nowEpochMilliseconds()
            val waitingMessages = findWaitingMessages(contactId)
            val expiredMessages =
                waitingMessages.filter { message ->
                    DirectPendingAuthorizationMessagePolicy.isExpired(
                        createdAtEpochMilliseconds = message.createdAtEpochMilliseconds,
                        nowEpochMilliseconds = nowEpochMilliseconds
                    )
                }

            discardMessages(expiredMessages)
            val expiredMessageIds = expiredMessages.mapTo(mutableSetOf(), MessageEntity::id)

            waitingMessages
                .filterNot { message -> message.id in expiredMessageIds }
                .forEach { message -> releaseMessage(message, contactId, contact) }
        }

    suspend fun discardWaitingForAuthorization(contactId: String): Result<Unit> =
        runCatching {
            discardMessages(findWaitingMessages(contactId))
        }

    suspend fun retry(messageId: String): Result<Unit> =
        runCatching {
            require(messageId.isNotBlank()) { "Message ID must not be blank" }

            val message = chatDao.findMessageById(messageId) ?: error("Message was not found")
            check(message.isMine) { "Only outgoing messages can be retried" }
            check(
                DirectMessageDeliveryStateMachine.canTransition(
                    current = message.deliveryStatus.toMessageDeliveryStatus(),
                    event = MessageDeliveryEvent.RETRY_REQUESTED
                )
            ) {
                "Only failed direct messages can be retried"
            }

            val target = loadTarget(message.conversationId)
            requireDirectChatAuthorization(target.contactId).getOrThrow()

            val packetId = message.packetId?.takeIf(String::isNotBlank) ?: error("Message has no linked protocol packet")

            protocolOutbox.resend(packetId).getOrThrow()
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

    private suspend fun releaseMessage(
        message: MessageEntity,
        contactId: String,
        contact: Contact
    ) {
        val nextStatus =
            DirectMessageDeliveryStateMachine.transition(
                current = message.deliveryStatus.toMessageDeliveryStatus(),
                event = MessageDeliveryEvent.AUTHORIZATION_GRANTED
            )
        check(nextStatus == MessageDeliveryStatus.QUEUED) {
            "Direct message is not waiting for authorization"
        }

        val packet =
            createPacket(
                messageId = message.id,
                text = message.text,
                attachments = attachmentTransfer.protocolAttachments(message.id),
                replyToMessageId = message.replyToMessageId
            )
        chatDao.upsertMessage(
            message.copy(
                packetId = packet.packetId,
                transportMode = contact.plannedTransportMode().name,
                deliveryStatus = nextStatus.name
            )
        )

        runCatching { enqueue(contactId, packet) }
            .onFailure { error ->
                logger.warn(error) {
                    "Queued direct message could not be released after authorization: messageId=${message.id}"
                }
            }
    }

    private suspend fun persistPreparedMessage(
        target: DirectTargetDto,
        contact: Contact,
        messageId: String,
        text: String,
        prepared: List<PreparedMessageAttachmentDto>,
        deliveryStatus: MessageDeliveryStatus,
        replyToMessageId: String?
    ) {
        val createdAtEpochMilliseconds = SystemClock.nowEpochMilliseconds()
        val message =
            MessageEntity(
                id = messageId,
                conversationId = target.conversationId,
                packetId = null,
                text = text,
                replyToMessageId = replyToMessageId,
                transportPayload = null,
                transportMode = contact.plannedTransportMode().name,
                contentStatus = MessageContentStatus.READABLE.name,
                deliveryStatus = deliveryStatus.name,
                senderContactId = null,
                isMine = true,
                createdAtEpochMilliseconds = createdAtEpochMilliseconds
            )

        try {
            chatDao.upsertMessage(message)
            attachmentTransfer.persistOutgoing(messageId, prepared)
            chatDao.updateConversationTimestamp(target.conversationId, createdAtEpochMilliseconds)
        } catch (error: Throwable) {
            runCatching { chatDao.deleteMessagesAndRefreshConversations(listOf(message)) }
            attachmentTransfer.cleanupPrepared(prepared)
            throw error
        }
    }

    private suspend fun linkPacket(
        messageId: String,
        packet: ChatMessagePacket,
        contact: Contact
    ) {
        val message = chatDao.findMessageById(messageId) ?: error("Stored direct message was not found")
        chatDao.upsertMessage(
            message.copy(
                packetId = packet.packetId,
                transportMode = contact.plannedTransportMode().name
            )
        )
    }

    private suspend fun discardMessages(messages: List<MessageEntity>) {
        if (messages.isEmpty()) return
        attachmentTransfer.deleteForMessages(messages.map(MessageEntity::id))
        chatDao.deleteMessagesAndRefreshConversations(messages)
    }

    private suspend fun findWaitingMessages(contactId: String): List<MessageEntity> =
        chatDao.findDirectMessagesByContactAndDeliveryStatus(
            contactId = contactId,
            deliveryStatus = MessageDeliveryStatus.WAITING_FOR_AUTHORIZATION.name
        )

    private suspend fun loadTarget(conversationId: String): DirectTargetDto {
        val conversation = chatDao.findConversationById(conversationId)
            ?: error("Direct conversation was not found")
        check(conversation.type == DIRECT_CONVERSATION_TYPE) { "Conversation is not direct" }
        val contactId = requireNotNull(conversation.contactId) { "Direct conversation has no contact" }
        return DirectTargetDto(conversationId = conversation.id, contactId = contactId)
    }

    private suspend fun createPacket(
        messageId: String,
        text: String,
        attachments: List<MessageAttachment>,
        replyToMessageId: String?
    ): ChatMessagePacket =
        ChatMessagePacket(
            packetId = IdGenerator.generate(prefix = "packet"),
            messageId = messageId,
            sentAtEpochMilliseconds = SystemClock.nowEpochMilliseconds(),
            text = text,
            attachments = attachments,
            replyToMessageId = replyToMessageId,
            senderPhoneNumber = localPhoneNumberProvider.getLocalPhoneNumber().getOrThrow(),
            profilePicture = localProfilePictureMetadataProvider.forMessage().getOrElse { ProfilePictureMetadata() }
        )

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

    private fun requireMessageContent(
        text: String,
        attachments: List<OutgoingMessageAttachment>
    ): String {
        MessageAttachmentPolicy.requireValid(attachments)
        return text.trim().also { normalizedText ->
            require(normalizedText.isNotEmpty() || attachments.isNotEmpty()) {
                "Message must contain text or attachments"
            }
        }
    }

    private fun Contact.plannedTransportMode(): TransportEncryptionMode {
        val identity = sparrowIdentity
        val canEncrypt =
            identity != null &&
                identity.encryptionPublicKey.isNotEmpty() &&
                identity.keyExchangeStatus == KeyExchangeStatus.MUTUAL
        return if (canEncrypt) TransportEncryptionMode.SEALED_BOX else TransportEncryptionMode.PLAINTEXT
    }

    private data class DirectTargetDto(
        val conversationId: String,
        val contactId: String
    )

    private companion object {
        const val DIRECT_CONVERSATION_TYPE = "DIRECT"
    }
}
