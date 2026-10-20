package com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.usecase

import com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.model.OperationConversation
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.model.StoredMessage
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.repository.DirectMessageRepository
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.repository.GroupMessageRepository

/** Loads the message an incoming operation refers to. */
class FindOperationTargetUseCase(
    private val directMessageRepository: DirectMessageRepository,
    private val groupMessageRepository: GroupMessageRepository
) {
    suspend operator fun invoke(conversation: OperationConversation, messageId: String): Result<StoredMessage?> =
        when (conversation) {
            OperationConversation.DIRECT -> directMessageRepository.find(messageId)
            OperationConversation.GROUP -> groupMessageRepository.find(messageId)
        }
}
