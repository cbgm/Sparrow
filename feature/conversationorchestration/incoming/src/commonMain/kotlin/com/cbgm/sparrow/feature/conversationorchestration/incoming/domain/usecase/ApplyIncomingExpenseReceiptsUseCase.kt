package com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.usecase

import com.cbgm.sparrow.core.messagepart.domain.model.Expense
import com.cbgm.sparrow.core.messagepart.domain.model.ExpenseBoardClosed
import com.cbgm.sparrow.core.messagepart.domain.model.ExpensePolicy
import com.cbgm.sparrow.core.messagepart.domain.model.MessagePart
import com.cbgm.sparrow.core.messagepart.domain.model.latestRevision
import com.cbgm.sparrow.core.messagepart.domain.model.revisionTime
import com.cbgm.sparrow.feature.attachments.domain.repository.MessageAttachmentOperationsRepository
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.repository.ExpenseReceiptRepository
import kotlinx.coroutines.flow.first

class ApplyIncomingExpenseReceiptsUseCase(
    private val attachments: MessageAttachmentOperationsRepository,
    private val receipts: ExpenseReceiptRepository
) {
    suspend operator fun invoke(
        groupId: String,
        parts: List<MessagePart>
    ): Result<Unit> = runCatching {
        val expenses = parts.filterIsInstance<Expense>()
        val hasRevisions = expenses.any { it.revisionOf != null }
        val stored = if (hasRevisions) attachments.observeExpenses(groupId).first() else emptyList()

        expenses.forEach { expense ->
            val previous = expense.revisionOf?.let(stored::latestRevision)
            val isCurrent = previous == null || expense.revisionTime >= previous.revisionTime
            val receipt = expense.receipt
            val bytes = receipt?.bytes
            if (isCurrent && receipt != null && bytes != null) {
                require(bytes.size in 1..ExpensePolicy.MAX_RECEIPT_BYTES) { "Invalid expense receipt size" }
                receipts.save(groupId, receipt.id, bytes).getOrThrow()
            }
            val replaced = previous?.receipt?.id?.takeIf { isCurrent && it != receipt?.id }
            if (replaced != null) receipts.delete(groupId, listOf(replaced)).getOrThrow()
        }

        if (parts.any { it is ExpenseBoardClosed }) receipts.deleteGroup(groupId).getOrThrow()
    }
}
