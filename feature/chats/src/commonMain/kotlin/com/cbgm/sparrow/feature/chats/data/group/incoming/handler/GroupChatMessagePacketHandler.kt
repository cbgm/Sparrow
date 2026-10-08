package com.cbgm.sparrow.feature.chats.data.group.incoming.handler

import com.cbgm.sparrow.core.logging.SparrowLog
import com.cbgm.sparrow.core.messagepart.data.mapper.toMessagePart
import com.cbgm.sparrow.core.messagepart.data.model.TextDto
import com.cbgm.sparrow.core.messagepart.domain.model.Poll
import com.cbgm.sparrow.core.messagepart.domain.model.PollPolicy
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
import com.cbgm.sparrow.feature.membership.domain.repository.GroupMembershipRepository
import com.cbgm.sparrow.feature.membership.domain.repository.GroupSecurityRepository
import com.cbgm.sparrow.protocol.handler.IncomingPacketContext
import com.cbgm.sparrow.protocol.message.GroupMessageContentCodec
import com.cbgm.sparrow.protocol.message.MessageOperation
import com.cbgm.sparrow.protocol.message.OperationMessage
import com.cbgm.sparrow.protocol.message.OperationMessageCodec
import com.cbgm.sparrow.protocol.outbox.ProtocolOutbox
import com.cbgm.sparrow.protocol.packet.DeliveryReceiptPacket
import com.cbgm.sparrow.protocol.packet.GroupChatMessagePacket
import com.cbgm.sparrow.protocol.packet.SparrowPacket
import com.cbgm.sparrow.protocol.profile.RemoteProfilePictureMetadataProcessor
import kotlinx.coroutines.flow.first

class GroupChatMessagePacketHandler(
    private val incomingMessageDataSource: IncomingMessageDataSource,
    private val protocolOutbox: ProtocolOutbox,
    private val groupSecurityManager: GroupSecurityRepository,
    private val groupMembershipRepository: GroupMembershipRepository,
    private val remoteProfilePictureMetadataProcessor: RemoteProfilePictureMetadataProcessor,
    private val groupMessageContentCodec: GroupMessageContentCodec,
    private val operationMessageCodec: OperationMessageCodec,
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
            val plaintext =
                groupSecurityManager
                    .decryptMessage(
                        packet = groupPacket,
                        senderContactId = context.contactId
                    ).getOrThrow()
            if (operationMessageCodec.canDecode(plaintext)) {
                handleOperation(
                    groupId = groupPacket.groupId,
                    senderContactId = context.contactId,
                    operationAtEpochMilliseconds = groupPacket.sentAtEpochMilliseconds,
                    message = operationMessageCodec.decode(plaintext)
                )
                return@runCatching
            }

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

            val content = groupMessageContentCodec.decode(plaintext)
            val text = content.parts.filterIsInstance<TextDto>().singleOrNull()?.text.orEmpty()
            val attachmentParts = content.parts.filterNot { part -> part is TextDto }

            if (existingMessage != null) {
                prefetchLinkPreviews(text)
                attachmentTransfer.persistIncoming(
                    messageId = groupPacket.messageId,
                    parts = attachmentParts.map { it.toMessagePart() },
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
                    logger.error(error) { "Could not store profile picture for ${context.contactId}" }
                }

            incomingMessageDataSource.saveMessage(
                message =
                    MessageEntity(
                        id = groupPacket.messageId,
                        conversationId = groupPacket.groupId,
                        packetId = groupPacket.packetId,
                        replyToMessageId = content.replyToMessageId,
                        transportPayload = context.encodedTransportPayload,
                        transportMode = GROUP_END_TO_END_ENCRYPTED_MODE,
                        contentStatus = MessageContentStatus.READABLE.name,
                        deliveryStatus = MessageDeliveryStatus.NOT_APPLICABLE.name,
                        senderContactId = context.contactId,
                        isMine = false,
                        createdAtEpochMilliseconds = groupPacket.sentAtEpochMilliseconds
                    ),
                text = text
            )
            prefetchLinkPreviews(text)
            attachmentTransfer.persistIncoming(
                messageId = groupPacket.messageId,
                parts = attachmentParts.map { it.toMessagePart() },
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

    private suspend fun handleOperation(
        groupId: String,
        senderContactId: String,
        operationAtEpochMilliseconds: Long,
        message: OperationMessage
    ) {
        when (val operation = message.operation) {
            is MessageOperation.Edit -> {
                val target = requireOperationTarget(groupId, operation.messageId)
                check(target.transportMode == GROUP_END_TO_END_ENCRYPTED_MODE) { "Only user messages can be edited" }
                check(!target.isMine && target.senderContactId == senderContactId) {
                    "Only the original sender can edit a group message"
                }
                check(!incomingMessageDataSource.findMessageText(operation.messageId).isNullOrBlank()) {
                    "Only text messages can be edited"
                }
                check(attachmentTransfer.messageParts(operation.messageId).getOrThrow().isEmpty()) {
                    "Messages with attachments cannot be edited"
                }
                incomingMessageDataSource.replaceMessageText(operation.messageId, operation.text.trim())
            }

            is MessageOperation.Delete -> {
                val target = requireOperationTarget(groupId, operation.messageId)
                check(target.transportMode == GROUP_END_TO_END_ENCRYPTED_MODE) { "Only user messages can be deleted" }
                check(!target.isMine && target.senderContactId == senderContactId) {
                    "Only the original sender can delete a group message"
                }
                attachmentTransfer.deleteForMessages(listOf(operation.messageId))
                incomingMessageDataSource.deleteMessages(listOf(target))
            }

            is MessageOperation.Reaction -> {
                val target = incomingMessageDataSource.findMessage(operation.messageId) ?: return
                check(target.conversationId == groupId) { "Reaction target belongs to another group" }
                if (operation.removed) {
                    incomingMessageDataSource.deleteReaction(operation.messageId, senderContactId, operation.emoji)
                } else {
                    incomingMessageDataSource.saveReaction(
                        MessageReactionEntity(operation.messageId, groupId, senderContactId, operation.emoji)
                    )
                }
            }

            is MessageOperation.PollVote -> {
                val target = requireOperationTarget(groupId, operation.messageId)
                check(target.transportMode == GROUP_END_TO_END_ENCRYPTED_MODE) { "Poll target is not a user message" }
                val poll = requirePoll(operation.messageId, operation.pollId)
                val updated =
                    PollPolicy.vote(
                        poll = poll,
                        voterId = senderContactId,
                        selectedOptionIds = operation.selectedOptionIds,
                        nowEpochMilliseconds = operationAtEpochMilliseconds
                    )
                attachmentTransfer.updateMessagePart(operation.messageId, updated).getOrThrow()
            }

            is MessageOperation.PollClose -> {
                val target = requireOperationTarget(groupId, operation.messageId)
                check(target.transportMode == GROUP_END_TO_END_ENCRYPTED_MODE) { "Poll target is not a user message" }
                val administration = groupMembershipRepository.observeAdministration(groupId).first()
                check(target.senderContactId == senderContactId || senderContactId in administration.adminContactIds) {
                    "Only the poll creator or a group admin can close the poll"
                }
                val updated = PollPolicy.close(requirePoll(operation.messageId, operation.pollId), operation.closedAtEpochMilliseconds)
                attachmentTransfer.updateMessagePart(operation.messageId, updated).getOrThrow()
            }
        }
    }

    private suspend fun requireOperationTarget(groupId: String, messageId: String): MessageEntity {
        val target = incomingMessageDataSource.findMessage(messageId) ?: error("Message was not found")
        check(target.conversationId == groupId) { "Operation target belongs to another group" }
        return target
    }

    private suspend fun requirePoll(messageId: String, pollId: String): Poll =
        attachmentTransfer
            .messageParts(messageId)
            .getOrThrow()
            .filterIsInstance<Poll>()
            .singleOrNull { poll -> poll.id == pollId }
            ?: error("Poll was not found")

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
