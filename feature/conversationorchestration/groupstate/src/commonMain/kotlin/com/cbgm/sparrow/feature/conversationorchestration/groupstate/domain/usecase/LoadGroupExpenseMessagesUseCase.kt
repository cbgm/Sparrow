package com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.usecase

import com.cbgm.sparrow.core.util.result.safeSuspendCall
import com.cbgm.sparrow.feature.attachments.domain.repository.MessageAttachmentOperationsRepository
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.model.GroupExpenseMessage
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.model.StoredMessage
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.repository.GroupMessageRepository

/** All stored messages of an expense board in send order, so a new member can replay the board's history. */
class LoadGroupExpenseMessagesUseCase(
    private val attachmentOperations: MessageAttachmentOperationsRepository,
    private val messageRepository: GroupMessageRepository
) {
    suspend operator fun invoke(groupId: String, boardId: String): Result<List<GroupExpenseMessage>> =
        safeSuspendCall(logFailure = false) {
            attachmentOperations.findExpenseMessageIds(groupId, boardId).getOrThrow()
                .mapNotNull { messageId -> messageRepository.find(messageId).getOrThrow() }
                .sortedBy(StoredMessage::createdAtEpochMilliseconds)
                .map { message ->
                    GroupExpenseMessage(
                        message = message,
                        text = messageRepository.findText(message.id).getOrThrow().orEmpty(),
                        parts = attachmentOperations.messageParts(message.id).getOrThrow()
                    )
                }
        }
}
