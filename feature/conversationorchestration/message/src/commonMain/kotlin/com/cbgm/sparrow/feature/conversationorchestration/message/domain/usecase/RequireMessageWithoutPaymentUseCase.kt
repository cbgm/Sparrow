package com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase

import com.cbgm.sparrow.core.base.messagepart.domain.model.PaymentTransfer
import com.cbgm.sparrow.core.util.result.safeSuspendCall
import com.cbgm.sparrow.feature.attachments.domain.repository.MessageAttachmentOperationsRepository

class RequireMessageWithoutPaymentUseCase(
    private val attachments: MessageAttachmentOperationsRepository
) {
    suspend operator fun invoke(messageId: String): Result<Unit> = safeSuspendCall(logFailure = false) {
        check(attachments.messageParts(messageId).getOrThrow().none { it is PaymentTransfer }) {
            "Payment records cannot be deleted individually"
        }
    }
}
