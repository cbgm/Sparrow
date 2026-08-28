package com.cbgm.sparrow.feature.chats.data.direct.incoming.handler

import com.cbgm.sparrow.core.crypto.transport.TransportEncryptionMode
import com.cbgm.sparrow.core.logging.SparrowLog
import com.cbgm.sparrow.core.protocol.handler.IncomingPacketContext
import com.cbgm.sparrow.core.protocol.outbox.ProtocolOutbox
import com.cbgm.sparrow.core.protocol.packet.ChatMessagePacket
import com.cbgm.sparrow.core.protocol.packet.DeliveryReceiptPacket
import com.cbgm.sparrow.core.protocol.profile.RemoteProfilePictureMetadataProcessor
import com.cbgm.sparrow.core.time.SystemClock
import com.cbgm.sparrow.data.database.dao.ChatDao
import com.cbgm.sparrow.data.database.dao.ContactDao
import com.cbgm.sparrow.data.database.entity.ConversationEntity
import com.cbgm.sparrow.data.database.entity.ConversationType
import com.cbgm.sparrow.data.database.entity.MessageEntity
import com.cbgm.sparrow.feature.attachments.data.datasource.MessageAttachmentDataSource
import com.cbgm.sparrow.feature.attachments.runtime.MessageAttachmentCacheCoordinator
import com.cbgm.sparrow.feature.chats.domain.model.MessageContentStatus
import com.cbgm.sparrow.feature.chats.domain.model.MessageDeliveryStatus

/** Direct-only incoming chat-message handler. */
class DirectMessagePacketHandler(
    private val chatDao: ChatDao,
    private val contactDao: ContactDao,
    private val protocolOutbox: ProtocolOutbox,
    private val remoteProfilePictureMetadataProcessor: RemoteProfilePictureMetadataProcessor,
    private val attachmentTransfer: MessageAttachmentDataSource,
    private val attachmentCacheCoordinator: MessageAttachmentCacheCoordinator
) {
    private val logger = SparrowLog.withTag("DirectMessagePacketHandler")

    suspend fun handle(
        context: IncomingPacketContext,
        packet: ChatMessagePacket
    ): Result<Unit> =
        runCatching {
            validateMessage(context, packet)
            remoteProfilePictureMetadataProcessor
                .apply(context.contactId, packet.profilePicture)
                .onFailure { error ->
                    logger.warn(error) { "Could not store profile picture for ${context.contactId}" }
                }
            updateSenderDisplayName(context.contactId, packet, context.receivedAtEpochMilliseconds)

            val conversation = getOrCreateConversation(context)
            storeMessage(conversation, context, packet)
            attachmentTransfer.persistIncoming(packet.messageId, packet.attachments)
            sendDeliveryReceipt(context.contactId, packet.messageId)
            attachmentCacheCoordinator.cache(packet.messageId)
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
        contactDao.usePhoneNumberAsDisplayNameWhenMissing(
            contactId = contactId,
            phoneNumber = phoneNumber,
            updatedAtEpochMilliseconds = receivedAt
        )
    }

    private suspend fun getOrCreateConversation(
        context: IncomingPacketContext
    ): ConversationEntity =
        chatDao.findConversationByContactId(contactId = context.contactId)
            ?: ConversationEntity(
                id = context.conversationId,
                contactId = context.contactId,
                type = ConversationType.DIRECT.name,
                title = null,
                createdAtEpochMilliseconds = context.receivedAtEpochMilliseconds,
                updatedAtEpochMilliseconds = context.receivedAtEpochMilliseconds
            )

    private suspend fun storeMessage(
        conversation: ConversationEntity,
        context: IncomingPacketContext,
        packet: ChatMessagePacket
    ) {
        chatDao.upsertIncomingChatMessage(
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
            transportPayload = context.encodedTransportPayload,
            transportMode = context.transportMode,
            contentStatus = MessageContentStatus.READABLE.name,
            deliveryStatus = MessageDeliveryStatus.NOT_APPLICABLE.name,
            isMine = false,
            senderContactId = context.contactId,
            createdAtEpochMilliseconds = sentAtEpochMilliseconds
        )

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
