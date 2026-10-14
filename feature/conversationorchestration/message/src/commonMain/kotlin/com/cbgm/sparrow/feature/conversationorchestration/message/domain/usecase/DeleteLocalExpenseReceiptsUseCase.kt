package com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase

import com.cbgm.sparrow.core.messagepart.domain.model.Expense
import com.cbgm.sparrow.core.messagepart.domain.model.expenseId
import com.cbgm.sparrow.feature.attachments.domain.repository.MessageAttachmentOperationsRepository
import com.cbgm.sparrow.feature.attachments.domain.repository.MessageAttachmentRepository
import kotlinx.coroutines.flow.first

/** Deletes the local receipt files of a group's expenses; the expense messages themselves stay. */
class DeleteLocalExpenseReceiptsUseCase(
    private val attachmentOperations: MessageAttachmentOperationsRepository,
    private val attachmentRepository: MessageAttachmentRepository
) {
    suspend fun forGroup(groupId: String): Result<Unit> = delete(groupId) { true }

    suspend fun forBoard(groupId: String, boardId: String): Result<Unit> =
        delete(groupId) { expense -> expense.boardId == boardId }

    suspend fun forExpense(groupId: String, expenseId: String, keepReceiptId: String? = null): Result<Unit> =
        delete(groupId) { expense -> expense.expenseId == expenseId && expense.receipt?.id != keepReceiptId }

    private suspend fun delete(groupId: String, selection: (Expense) -> Boolean): Result<Unit> =
        runCatching {
            val receiptIds =
                attachmentOperations.observeExpenses(groupId).first()
                    .filter(selection)
                    .mapNotNullTo(mutableSetOf()) { expense -> expense.receipt?.id }
            if (receiptIds.isNotEmpty()) attachmentRepository.deleteLocalAttachments(receiptIds).getOrThrow()
        }
}
