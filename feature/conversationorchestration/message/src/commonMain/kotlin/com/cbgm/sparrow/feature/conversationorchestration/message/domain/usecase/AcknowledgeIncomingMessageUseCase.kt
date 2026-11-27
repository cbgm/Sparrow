package com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase

import com.cbgm.sparrow.core.util.result.safeSuspendCall
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.model.UnreadIncomingMessage
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.repository.DirectMessageRepository
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.repository.GroupMessageRepository

/**
 * Sends the read receipt for an incoming message and records that it was sent, so the receipt is not sent
 * again the next time the chat is read.
 */
class AcknowledgeIncomingMessageUseCase(
    private val directMessageRepository: DirectMessageRepository,
    private val groupMessageRepository: GroupMessageRepository,
    private val sendReadReceipt: SendReadReceiptUseCase
) {
    suspend fun forDirectMessage(message: UnreadIncomingMessage): Result<Unit> =
        acknowledge(message, notMarked = "Incoming direct message could not be marked as read") {
            directMessageRepository.markReadReceiptSent(message.messageId).getOrThrow()
        }

    suspend fun forGroupMessage(message: UnreadIncomingMessage): Result<Unit> =
        acknowledge(message, notMarked = "incoming group message could not be marked as read") {
            groupMessageRepository.markReadReceiptSent(message.messageId).getOrThrow()
        }

    private suspend fun acknowledge(
        message: UnreadIncomingMessage,
        notMarked: String,
        markReceiptSent: suspend () -> Boolean
    ): Result<Unit> =
        safeSuspendCall(logFailure = false) {
            sendReadReceipt(message.messageId, message.contactId).getOrThrow()
            check(markReceiptSent()) { notMarked }
        }
}
