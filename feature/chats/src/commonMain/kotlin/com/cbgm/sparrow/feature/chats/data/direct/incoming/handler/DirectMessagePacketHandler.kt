package com.cbgm.sparrow.feature.chats.data.direct.incoming.handler

import com.cbgm.sparrow.core.crypto.transport.TransportEncryptionMode
import com.cbgm.sparrow.core.logging.SparrowLog
import com.cbgm.sparrow.core.protocol.handler.IncomingPacketContext
import com.cbgm.sparrow.core.protocol.outbox.ProtocolOutbox
import com.cbgm.sparrow.core.protocol.packet.ChatMessagePacket
import com.cbgm.sparrow.core.protocol.packet.DeliveryReceiptPacket
import com.cbgm.sparrow.core.protocol.profile.RemoteProfilePictureMetadataProcessor
import com.cbgm.sparrow.core.time.SystemClock
import com.cbgm.sparrow.data.database.entity.ConversationEntity
import com.cbgm.sparrow.data.database.entity.MessageEntity
import com.cbgm.sparrow.data.database.entity.MessageReactionEntity
import com.cbgm.sparrow.feature.attachments.domain.model.AttachmentMessageContext
import com.cbgm.sparrow.feature.attachments.domain.repository.MessageAttachmentOperationsRepository
import com.cbgm.sparrow.feature.autoreply.domain.usecase.ClaimAutoReplyForContactUseCase
import com.cbgm.sparrow.feature.autoreply.domain.usecase.ReleaseAutoReplyRecipientUseCase
import com.cbgm.sparrow.feature.chats.data.datasource.MessageReactionDataSource
import com.cbgm.sparrow.feature.chats.data.direct.datasource.DirectConversationDataSource
import com.cbgm.sparrow.feature.chats.data.direct.outgoing.DirectOutgoingMessageProcessor
import com.cbgm.sparrow.feature.chats.domain.model.MessageContentStatus
import com.cbgm.sparrow.feature.chats.domain.model.MessageDeliveryStatus
import com.cbgm.sparrow.feature.contacts.domain.repository.ContactRepository

/** Direct-only incoming chat-message handler. */
class DirectMessagePacketHandler(
    private val conversationDataSource: DirectConversationDataSource,
    private val contactRepository: ContactRepository,
    private val messageReactionDataSource: MessageReactionDataSource,
    private val protocolOutbox: ProtocolOutbox,
    private val remoteProfilePictureMetadataProcessor: RemoteProfilePictureMetadataProcessor,
    private val attachmentTransfer: MessageAttachmentOperationsRepository,
    private val claimAutoReplyForContact: ClaimAutoReplyForContactUseCase,
    private val releaseAutoReplyRecipient: ReleaseAutoReplyRecipientUseCase,
    private val outgoingMessageProcessor: DirectOutgoingMessageProcessor
) {
    private val logger = SparrowLog.withTag("DirectMessagePacketHandler")

    suspend fun handle(
        context: IncomingPacketContext,
        packet: ChatMessagePacket
    ): Result<Unit> =
        runCatching {
            packet.reaction?.let { reaction ->
                val target = conversationDataSource.findMessageById(reaction.messageId) ?: return@runCatching
                check(target.conversationId == context.conversationId) { "Reaction target belongs to another conversation" }
                if (reaction.removed) {
                    messageReactionDataSource.delete(reaction.messageId, context.contactId, reaction.emoji)
                } else {
                    messageReactionDataSource.upsert(
                        MessageReactionEntity(reaction.messageId, context.conversationId, context.contactId, reaction.emoji)
                    )
                }
                return@runCatching
            }
            validateMessage(context, packet)
            remoteProfilePictureMetadataProcessor
                .apply(context.contactId, packet.profilePicture)
                .onFailure { error ->
                    logger.error(error) { "Could not store profile picture for ${context.contactId}" }
                }
            updateSenderDisplayName(context.contactId, packet, context.receivedAtEpochMilliseconds)

            val conversation = getOrCreateConversation(context)
            storeMessage(conversation, context, packet)
            attachmentTransfer.persistIncoming(
                messageId = packet.messageId,
                attachments = packet.attachments,
                context = AttachmentMessageContext(
                    conversationId = conversation.id,
                    createdAtEpochMilliseconds = conversationDataSource.findMessageById(packet.messageId)
                        ?.createdAtEpochMilliseconds ?: context.receivedAtEpochMilliseconds,
                    displayName = contactRepository.getContact(context.contactId).getOrThrow()?.let { contact ->
                        contact.displayName?.takeIf(String::isNotBlank)
                            ?: contact.preferredPhoneNumber?.value
                    } ?: packet.senderPhoneNumber?.takeIf(String::isNotBlank) ?: context.contactId,
                    isGroup = false,
                    isMine = false,
                    senderContactId = context.contactId
                )
            )
            sendDeliveryReceipt(context.contactId, packet.messageId)
            sendAutoReplyIfNeeded(conversation.id, context.contactId)
            attachmentTransfer.cacheIncoming(packet.messageId)
        }

    private fun validateMessage(
        context: IncomingPacketContext,
        packet: ChatMessagePacket
    ) {
        require(packet.text.isNotBlank() || packet.attachments.isNotEmpty()) {
            "Incoming chat message must contain text or attachments"
        }
        require(
            packet.attachments.isEmpty() ||
                context.transportMode == TransportEncryptionMode.SEALED_BOX.name
        ) {
            "Direct message attachments require an encrypted Sparrow transport"
        }
    }

    private suspend fun updateSenderDisplayName(
        contactId: String,
        packet: ChatMessagePacket,
        receivedAt: Long
    ) {
        val phoneNumber = packet.senderPhoneNumber?.trim()?.takeIf(String::isNotBlank) ?: return
        contactRepository.usePhoneNumberAsDisplayNameWhenMissing(
            contactId = contactId,
            phoneNumber = phoneNumber,
            updatedAtEpochMilliseconds = receivedAt
        ).getOrThrow()
    }

    private suspend fun getOrCreateConversation(
        context: IncomingPacketContext
    ): ConversationEntity =
        conversationDataSource.findConversationById(context.conversationId)
            ?: conversationDataSource.getOrCreate(context.contactId)

    private suspend fun storeMessage(
        conversation: ConversationEntity,
        context: IncomingPacketContext,
        packet: ChatMessagePacket
    ) {
        conversationDataSource.upsertIncomingChatMessage(
            conversation = conversation,
            message = packet.toMessageEntity(conversation.id, context),
            timestamp = context.receivedAtEpochMilliseconds
        )
    }

    private fun ChatMessagePacket.toMessageEntity(
        conversationId: String,
        context: IncomingPacketContext
    ): MessageEntity =
        MessageEntity(
            id = messageId,
            conversationId = conversationId,
            packetId = packetId,
            text = text,
            replyToMessageId = replyToMessageId,
            transportPayload = context.encodedTransportPayload,
            transportMode = context.transportMode,
            contentStatus = MessageContentStatus.READABLE.name,
            deliveryStatus = MessageDeliveryStatus.NOT_APPLICABLE.name,
            isMine = false,
            senderContactId = context.contactId,
            createdAtEpochMilliseconds = context.receivedAtEpochMilliseconds
        )

    private suspend fun sendAutoReplyIfNeeded(
        conversationId: String,
        contactId: String
    ) {
        val claimedReply = claimAutoReplyForContact(contactId)
            .onFailure { failure -> logger.error(failure) { "Could not claim automatic reply for $contactId" } }
            .getOrNull() ?: return

        val sendResult =
            outgoingMessageProcessor.send(
                conversationId = conversationId,
                text = claimedReply.text,
                attachments = emptyList(),
                replyToMessageId = null
            )

        if (sendResult.isSuccess) return

        logger.error(sendResult.exceptionOrNull()) {
            "Auto reply could not be sent to contactId=$contactId"
        }

        val activationSessionId = claimedReply.activationSessionId ?: return
        runCatching {
            releaseAutoReplyRecipient(
                contactId = contactId,
                expectedActivationSessionId = activationSessionId
            ).getOrThrow()
        }.onFailure { error ->
            logger.error(error) {
                "Could not release failed auto-reply claim for contactId=$contactId"
            }
        }
    }

    private suspend fun sendDeliveryReceipt(
        contactId: String,
        messageId: String
    ) {
        protocolOutbox
            .enqueue(
                contactId = contactId,
                packet =
                    DeliveryReceiptPacket(
                        packetId = "delivery-receipt-$messageId",
                        messageId = messageId,
                        deliveredAtEpochMilliseconds = SystemClock.nowEpochMilliseconds()
                    )
            ).getOrThrow()

        logger.debug {
            "Direct delivery receipt queued: messageId=$messageId, contactId=$contactId"
        }
    }
}
