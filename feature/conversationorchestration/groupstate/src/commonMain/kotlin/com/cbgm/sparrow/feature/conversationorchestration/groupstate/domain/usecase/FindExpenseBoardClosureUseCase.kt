package com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.usecase

import com.cbgm.sparrow.core.base.messagepart.domain.model.ExpenseBoardClosed
import com.cbgm.sparrow.core.util.result.safeSuspendCall
import com.cbgm.sparrow.feature.attachments.domain.repository.MessageAttachmentOperationsRepository
import kotlinx.coroutines.flow.first

/**
 * The stored closing announcement of an expense board, if any. Closing the board again after a failure must not
 * announce it a second time.
 */
class FindExpenseBoardClosureUseCase(
    private val attachmentRepository: MessageAttachmentOperationsRepository
) {
    suspend operator fun invoke(groupId: String, boardId: String): Result<ExpenseBoardClosed?> =
        safeSuspendCall(logFailure = false) {
            attachmentRepository.observeExpenseBoardClosures(groupId).first()
                .firstOrNull { closure -> closure.boardId == boardId }
        }
}
