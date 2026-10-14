package com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase

import com.cbgm.sparrow.core.id.IdGenerator
import com.cbgm.sparrow.core.message.domain.model.MessageContentStatus
import com.cbgm.sparrow.core.message.domain.model.MessageDeliveryStatus
import com.cbgm.sparrow.core.message.domain.model.MessageTransportMode
import com.cbgm.sparrow.core.result.safeSuspendCall
import com.cbgm.sparrow.core.time.SystemClock
import com.cbgm.sparrow.feature.attachments.domain.model.AttachmentMessageContext
import com.cbgm.sparrow.feature.attachments.domain.repository.MessageAttachmentOperationsRepository
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.model.OutgoingMessageContent
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.model.StoredMessage
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.model.StoredOutgoingMessage
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.repository.GroupMessageRepository

/** Persists a new queued group message and its attachments; on attachment failure the row is removed again. */
class StoreOutgoingGroupMessageUseCase(
    private val messageRepository: GroupMessageRepository,
    private val attachmentRepository: MessageAttachmentOperationsRepository
) {
    suspend operator fun invoke(
        groupId: String,
        content: OutgoingMessageContent,
        replyToMessageId: String?
    ): Result<StoredOutgoingMessage> =
        safeSuspendCall(logFailure = false) {
            val message =
                StoredMessage(
                    id = IdGenerator.generate(prefix = "group-message"),
                    conversationId = groupId,
                    packetId = null,
                    replyToMessageId = replyToMessageId,
                    transportPayload = null,
                    transportMode = MessageTransportMode.GROUP_END_TO_END_ENCRYPTED,
                    contentStatus = MessageContentStatus.READABLE,
                    deliveryStatus = MessageDeliveryStatus.QUEUED,
                    senderContactId = null,
                    readReceiptSent = false,
                    isMine = true,
                    createdAtEpochMilliseconds = SystemClock.nowEpochMilliseconds()
                )
            messageRepository.saveOutgoing(message, content.text, recipientStates = emptyList()).getOrThrow()
            val parts =
                safeSuspendCall(logFailure = false) {
                    val conversation = messageRepository.findConversation(groupId).getOrThrow()
                        ?: error("Group conversation was not found")
                    attachmentRepository.persistOutgoing(
                        messageId = message.id,
                        parts = content.attachments,
                        context = AttachmentMessageContext(
                            conversationId = groupId,
                            createdAtEpochMilliseconds = message.createdAtEpochMilliseconds,
                            displayName = conversation.title?.takeIf(String::isNotBlank) ?: conversation.id,
                            isGroup = true,
                            isMine = true,
                            senderContactId = null
                        )
                    ).getOrThrow()
                }.getOrElse { error ->
                    messageRepository.delete(listOf(message))
                    throw error
                }
            StoredOutgoingMessage(message = message, text = content.text, parts = parts)
        }
}
