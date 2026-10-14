package com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.usecase

import com.cbgm.sparrow.core.messagepart.domain.model.Expense
import com.cbgm.sparrow.core.messagepart.domain.model.ExpenseBoardClosed
import com.cbgm.sparrow.core.messagepart.domain.model.MessagePart
import com.cbgm.sparrow.core.messagepart.domain.model.expenseId
import com.cbgm.sparrow.feature.attachments.domain.repository.MessageAttachmentOperationsRepository
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase.DeleteLocalExpenseReceiptsUseCase
import kotlinx.coroutines.flow.first

class DeleteReplacedExpenseReceiptsUseCase(
    private val attachmentOperations: MessageAttachmentOperationsRepository,
    private val deleteLocalExpenseReceipts: DeleteLocalExpenseReceiptsUseCase
) {
    suspend operator fun invoke(groupId: String, parts: List<MessagePart>): Result<Unit> =
        runCatching {
            val revisions = parts.filterIsInstance<Expense>().filter { it.revisionOf != null }
            if (revisions.isNotEmpty()) {
                val stored = attachmentOperations.observeExpenses(groupId).first()
                revisions.forEach { revision ->
                    val newestStored =
                        stored.filter { it.expenseId == revision.expenseId }
                            .maxOfOrNull { it.revisionTime() } ?: 0L
                    if (revision.revisionTime() >= newestStored) {
                        deleteLocalExpenseReceipts.forExpense(
                            groupId = groupId,
                            expenseId = revision.expenseId,
                            keepReceiptId = revision.receipt?.id
                        ).getOrThrow()
                    }
                }
            }
            parts.filterIsInstance<ExpenseBoardClosed>().forEach { closure ->
                deleteLocalExpenseReceipts.forBoard(groupId, closure.boardId).getOrThrow()
            }
        }

    private fun Expense.revisionTime(): Long = deletedAtEpochMilliseconds ?: editedAtEpochMilliseconds ?: 0L
}
