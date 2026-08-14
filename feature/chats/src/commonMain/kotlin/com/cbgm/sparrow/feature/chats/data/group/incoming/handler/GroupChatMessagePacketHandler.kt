package com.cbgm.sparrow.feature.chats.data.group.incoming.handler

import com.cbgm.sparrow.core.protocol.handler.IncomingPacketContext
import com.cbgm.sparrow.core.protocol.outbox.ProtocolOutbox
import com.cbgm.sparrow.core.protocol.packet.DeliveryReceiptPacket
import com.cbgm.sparrow.core.protocol.packet.GroupChatMessagePacket
import com.cbgm.sparrow.core.protocol.packet.SparrowPacket
import com.cbgm.sparrow.core.time.SystemClock
import com.cbgm.sparrow.data.database.dao.ChatDao
import com.cbgm.sparrow.data.database.entity.MessageEntity
import com.cbgm.sparrow.feature.chats.data.group.security.GROUP_END_TO_END_ENCRYPTED_MODE
import com.cbgm.sparrow.feature.chats.data.group.security.GroupSecurityManager
import com.cbgm.sparrow.feature.chats.domain.model.MessageContentStatus
import com.cbgm.sparrow.feature.chats.domain.model.MessageDeliveryStatus

class GroupChatMessagePacketHandler(
    private val chatDao: ChatDao,
    private val protocolOutbox: ProtocolOutbox,
    private val groupSecurityManager: GroupSecurityManager
) : GroupPacketHandler {
    override fun canHandle(packet: SparrowPacket): Boolean = packet is GroupChatMessagePacket

    override suspend fun handle(
        context: IncomingPacketContext,
        packet: SparrowPacket
    ): Result<Unit> =
        runCatching {
            val groupPacket =
                packet as? GroupChatMessagePacket
                    ?: error("GroupChatMessagePacketHandler received an incompatible packet")
            val conversation =
                chatDao.findConversationById(groupPacket.groupId)
                    ?: error("Group conversation was not found")
            check(conversation.type == GROUP_CONVERSATION_TYPE) { "Conversation is not a group" }
            val existingMessage = chatDao.findMessageById(groupPacket.messageId)

            if (existingMessage != null) {
                check(
                    existingMessage.conversationId == groupPacket.groupId &&
                        existingMessage.packetId == groupPacket.packetId &&
                        existingMessage.senderContactId == context.contactId
                ) {
                    "Group message ID conflicts with an existing message"
                }

                queueDeliveryReceipt(groupPacket, context.contactId)
                return@runCatching
            }

            val plaintext =
                groupSecurityManager
                    .decryptMessage(
                        packet = groupPacket,
                        senderContactId = context.contactId
                    ).getOrThrow()

            chatDao.upsertMessage(
                MessageEntity(
                    id = groupPacket.messageId,
                    conversationId = groupPacket.groupId,
                    packetId = groupPacket.packetId,
                    text = plaintext,
                    transportPayload = context.encodedTransportPayload,
                    transportMode = GROUP_END_TO_END_ENCRYPTED_MODE,
                    contentStatus = MessageContentStatus.READABLE.name,
                    deliveryStatus = MessageDeliveryStatus.NOT_APPLICABLE.name,
                    senderContactId = context.contactId,
                    isMine = false,
                    createdAtEpochMilliseconds = groupPacket.sentAtEpochMilliseconds
                )
            )
            chatDao.updateConversationTimestamp(groupPacket.groupId, context.receivedAtEpochMilliseconds)

            queueDeliveryReceipt(groupPacket, context.contactId)
        }

    private suspend fun queueDeliveryReceipt(
        packet: GroupChatMessagePacket,
        contactId: String
    ) {
        protocolOutbox
            .enqueue(
                contactId = contactId,
                packet =
                    DeliveryReceiptPacket(
                        packetId = "delivery-receipt-${packet.messageId}-$contactId",
                        messageId = packet.messageId,
                        deliveredAtEpochMilliseconds = SystemClock.nowEpochMilliseconds()
                    )
            ).getOrThrow()
    }

    private companion object {
        const val GROUP_CONVERSATION_TYPE = "GROUP"
    }
}
