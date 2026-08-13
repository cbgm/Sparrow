package com.cbgm.securechat.feature.chats.data.direct.incoming.handler

import com.cbgm.securechat.core.logging.SecureChatLog
import com.cbgm.securechat.core.protocol.handler.IncomingPacketContext
import com.cbgm.securechat.core.protocol.outbox.ProtocolOutbox
import com.cbgm.securechat.core.protocol.packet.ChatMessagePacket
import com.cbgm.securechat.core.protocol.packet.DeliveryReceiptPacket
import com.cbgm.securechat.core.time.SystemClock
import com.cbgm.securechat.data.database.dao.ChatDao
import com.cbgm.securechat.data.database.dao.ContactDao
import com.cbgm.securechat.data.database.entity.ConversationEntity
import com.cbgm.securechat.data.database.entity.ConversationType
import com.cbgm.securechat.data.database.entity.MessageEntity
import com.cbgm.securechat.feature.chats.domain.model.MessageContentStatus
import com.cbgm.securechat.feature.chats.domain.model.MessageDeliveryStatus
import com.cbgm.securechat.feature.contacts.domain.identity.IdentityInvitationService

/** Direct-only incoming chat-message handler. */
class DirectMessagePacketHandler(
    private val chatDao: ChatDao,
    private val contactDao: ContactDao,
    private val protocolOutbox: ProtocolOutbox,
    private val identityInvitationService: IdentityInvitationService
) {
    private val logger = SecureChatLog.withTag("DirectMessagePacketHandler")

    suspend fun handle(
        context: IncomingPacketContext,
        packet: ChatMessagePacket
    ): Result<Unit> =
        runCatching {
            validateMessage(context.contactId, packet)
            updateSenderDisplayName(context.contactId, packet, context.receivedAtEpochMilliseconds)

            val conversation = getOrCreateConversation(context)
            storeMessage(conversation, context, packet)
            sendDeliveryReceipt(context.contactId, packet.messageId)
        }

    private suspend fun validateMessage(
        contactId: String,
        packet: ChatMessagePacket
    ) {
        require(packet.text.isNotBlank()) { "Incoming chat message must not be blank" }
        identityInvitationService.requireDirectChatAuthorization(contactId).getOrThrow()
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
