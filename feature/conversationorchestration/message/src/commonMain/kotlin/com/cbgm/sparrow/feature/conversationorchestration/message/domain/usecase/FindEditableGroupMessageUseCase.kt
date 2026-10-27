package com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase

import com.cbgm.sparrow.core.base.message.domain.model.MessageDeliveryStatus
import com.cbgm.sparrow.core.util.result.safeSuspendCall
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.model.StoredMessage
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.repository.GroupMessageRepository

/** Loads an own group text message that no recipient has read yet; only such messages can be edited. */
class FindEditableGroupMessageUseCase(
    private val messageRepository: GroupMessageRepository,
    private val findGroupMessage: FindGroupMessageUseCase,
    private val checkMessageEditable: CheckMessageEditableUseCase
) {
    suspend operator fun invoke(groupId: String, messageId: String): Result<StoredMessage> =
        safeSuspendCall(logFailure = false) {
            val message = findGroupMessage(groupId, messageId).getOrThrow()
            check(message.isGroupUserMessage) { "Only user messages can be edited" }
            check(message.isMine) { "Only your own messages can be edited" }
            checkMessageEditable.forGroupMessage(message.id).getOrThrow()?.let { reason -> error(reason) }
            check(
                messageRepository.findRecipientStates(message.id).getOrThrow()
                    .none { state -> state.deliveryStatus == MessageDeliveryStatus.READ }
            ) { "Read messages cannot be edited" }
            message
        }
}
