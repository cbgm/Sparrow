package com.cbgm.sparrow.feature.chats.data.group.incoming.handler

import com.cbgm.sparrow.core.logging.SparrowLog
import com.cbgm.sparrow.core.protocol.handler.IncomingPacketContext
import com.cbgm.sparrow.core.protocol.message.GroupMessageContentCodec
import com.cbgm.sparrow.core.protocol.outbox.ProtocolOutbox
import com.cbgm.sparrow.core.protocol.packet.DeliveryReceiptPacket
import com.cbgm.sparrow.core.protocol.packet.GroupChatMessagePacket
import com.cbgm.sparrow.core.protocol.packet.SparrowPacket
import com.cbgm.sparrow.core.protocol.profile.RemoteProfilePictureMetadataProcessor
import com.cbgm.sparrow.core.time.SystemClock
import com.cbgm.sparrow.data.database.entity.MessageEntity
import com.cbgm.sparrow.data.database.entity.MessageReactionEntity
import com.cbgm.sparrow.feature.attachments.domain.model.AttachmentMessageContext
import com.cbgm.sparrow.feature.attachments.domain.repository.MessageAttachmentOperationsRepository
import com.cbgm.sparrow.feature.chats.data.datasource.IncomingMessageDataSource
import com.cbgm.sparrow.feature.chats.data.group.security.GROUP_END_TO_END_ENCRYPTED_MODE
import com.cbgm.sparrow.feature.chats.domain.model.MessageContentStatus
import com.cbgm.sparrow.feature.chats.domain.model.MessageDeliveryStatus
import com.cbgm.sparrow.feature.linkpreview.domain.usecase.PrefetchLinkPreviewsUseCase
import com.cbgm.sparrow.feature.membership.domain.repository.GroupSecurityRepository

class GroupChatMessagePacketHandler(
    private val incomingMessageDataSource: IncomingMessageDataSource,
    private val protocolOutbox: ProtocolOutbox,
    private val groupSecurityManager: GroupSecurityRepository,
    private val remoteProfilePictureMetadataProcessor: RemoteProfilePictureMetadataProcessor,
    private val groupMessageContentCodec: GroupMessageContentCodec,
    private val attachmentTransfer: MessageAttachmentOperationsRepository,
    private val prefetchLinkPreviews: PrefetchLinkPreviewsUseCase
) : GroupPacketHandler {
    private val logger = SparrowLog.withTag("GroupChatMessagePacketHandler")

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
                incomingMessageDataSource.findConversation(groupPacket.groupId)
                    ?: error("Group conversation was not found")
            check(conversation.type == GROUP_CONVERSATION_TYPE) { "Conversation is not a group" }
            val existingMessage = incomingMessageDataSource.findMessage(groupPacket.messageId)
            if (existingMessage != null) {
                check(
                    existingMessage.conversationId == groupPacket.groupId &&
                        existingMessage.packetId == groupPacket.packetId &&
                        existingMessage.senderContactId == context.contactId
                ) {
                    "Group message ID conflicts with an existing message"
                }
            }

            val plaintext =
                groupSecurityManager
                    .decryptMessage(
                        packet = groupPacket,
                        senderContactId = context.contactId
                    ).getOrThrow()
            val content = groupMessageContentCodec.decode(plaintext)

            content.reaction?.let { reaction ->
                val target = incomingMessageDataSource.findMessage(reaction.messageId) ?: return@runCatching
                check(target.conversationId == groupPacket.groupId) { "Reaction target belongs to another group" }
                if (reaction.removed) {
                    incomingMessageDataSource.deleteReaction(reaction.messageId, context.contactId, reaction.emoji)
                } else {
                    incomingMessageDataSource.saveReaction(
                        MessageReactionEntity(reaction.messageId, groupPacket.groupId, context.contactId, reaction.emoji)
                    )
                }
                return@runCatching
            }

            if (existingMessage != null) {
                prefetchLinkPreviews(content.text)
                attachmentTransfer.persistIncoming(
                    messageId = groupPacket.messageId,
                    attachments = content.attachments,
                    context = AttachmentMessageContext(
                        conversationId = conversation.id,
                        createdAtEpochMilliseconds = existingMessage.createdAtEpochMilliseconds,
                        displayName = conversation.title?.takeIf(String::isNotBlank) ?: conversation.id,
                        isGroup = true,
                        isMine = false,
                        senderContactId = context.contactId
                    )
                )
                queueDeliveryReceipt(groupPacket, context.contactId)
                attachmentTransfer.cacheIncoming(groupPacket.messageId)
                return@runCatching
            }

            remoteProfilePictureMetadataProcessor
                .apply(context.contactId, groupPacket.profilePicture)
                .onFailure { error ->
                    logger.warn(error) { "Could not store profile picture for ${context.contactId}" }
                }

            incomingMessageDataSource.saveMessage(
                MessageEntity(
                    id = groupPacket.messageId,
                    conversationId = groupPacket.groupId,
                    packetId = groupPacket.packetId,
                    text = content.text,
                    replyToMessageId = content.replyToMessageId,
                    transportPayload = context.encodedTransportPayload,
                    transportMode = GROUP_END_TO_END_ENCRYPTED_MODE,
                    contentStatus = MessageContentStatus.READABLE.name,
                    deliveryStatus = MessageDeliveryStatus.NOT_APPLICABLE.name,
                    senderContactId = context.contactId,
                    isMine = false,
                    createdAtEpochMilliseconds = groupPacket.sentAtEpochMilliseconds
                )
            )
            prefetchLinkPreviews(content.text)
            attachmentTransfer.persistIncoming(
                messageId = groupPacket.messageId,
                attachments = content.attachments,
                context = AttachmentMessageContext(
                    conversationId = conversation.id,
                    createdAtEpochMilliseconds = groupPacket.sentAtEpochMilliseconds,
                    displayName = conversation.title?.takeIf(String::isNotBlank) ?: conversation.id,
                    isGroup = true,
                    isMine = false,
                    senderContactId = context.contactId
                )
            )
            incomingMessageDataSource.updateConversationTimestamp(groupPacket.groupId, context.receivedAtEpochMilliseconds)

            queueDeliveryReceipt(groupPacket, context.contactId)
            attachmentTransfer.cacheIncoming(groupPacket.messageId)
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
