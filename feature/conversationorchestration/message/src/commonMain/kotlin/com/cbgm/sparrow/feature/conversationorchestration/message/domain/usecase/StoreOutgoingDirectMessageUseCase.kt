package com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase

import com.cbgm.sparrow.core.base.message.domain.model.MessageContentStatus
import com.cbgm.sparrow.core.base.message.domain.model.MessageDeliveryStatus
import com.cbgm.sparrow.core.crypto.transport.TransportEncryptionMode
import com.cbgm.sparrow.core.util.id.IdGenerator
import com.cbgm.sparrow.core.util.result.safeSuspendCall
import com.cbgm.sparrow.core.util.time.SystemClock
import com.cbgm.sparrow.feature.attachments.domain.model.AttachmentMessageContext
import com.cbgm.sparrow.feature.attachments.domain.repository.MessageAttachmentOperationsRepository
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.model.DirectRecipient
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.model.OutgoingMessageContent
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.model.StoredMessage
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.model.StoredOutgoingMessage
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.repository.DirectMessageRepository

/** Persists a new outgoing direct message and its attachments; on attachment failure the row is removed again. */
class StoreOutgoingDirectMessageUseCase(
    private val messageRepository: DirectMessageRepository,
    private val attachmentRepository: MessageAttachmentOperationsRepository
) {
    suspend operator fun invoke(
        recipient: DirectRecipient,
        content: OutgoingMessageContent,
        deliveryStatus: MessageDeliveryStatus,
        replyToMessageId: String?
    ): Result<StoredOutgoingMessage> =
        safeSuspendCall(logFailure = false) {
            val message =
                StoredMessage(
                    id = IdGenerator.generate(prefix = "message"),
                    conversationId = recipient.target.conversationId,
                    packetId = null,
                    replyToMessageId = replyToMessageId,
                    transportPayload = null,
                    transportMode = TransportEncryptionMode.SEALED_BOX.name,
                    contentStatus = MessageContentStatus.READABLE,
                    deliveryStatus = deliveryStatus,
                    senderContactId = null,
                    readReceiptSent = false,
                    isMine = true,
                    createdAtEpochMilliseconds = SystemClock.nowEpochMilliseconds()
                )
            messageRepository.saveWithText(message, content.text).getOrThrow()
            val parts =
                attachmentRepository.persistOutgoing(
                    messageId = message.id,
                    parts = content.attachments,
                    context = AttachmentMessageContext(
                        conversationId = message.conversationId,
                        createdAtEpochMilliseconds = message.createdAtEpochMilliseconds,
                        displayName = recipient.displayName,
                        isGroup = false,
                        isMine = true,
                        senderContactId = null
                    )
                ).getOrElse { error ->
                    messageRepository.delete(listOf(message))
                    throw error
                }
            StoredOutgoingMessage(message = message, text = content.text, parts = parts)
        }
}
