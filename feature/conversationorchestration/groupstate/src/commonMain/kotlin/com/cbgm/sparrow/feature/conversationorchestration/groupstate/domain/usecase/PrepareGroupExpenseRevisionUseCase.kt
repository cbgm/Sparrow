package com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.usecase

import com.cbgm.sparrow.core.base.messagepart.domain.model.Expense
import com.cbgm.sparrow.core.base.messagepart.domain.model.ExpensePolicy
import com.cbgm.sparrow.core.base.messagepart.domain.model.Image
import com.cbgm.sparrow.core.base.messagepart.domain.model.expenseId
import com.cbgm.sparrow.core.util.id.IdGenerator
import com.cbgm.sparrow.core.util.result.safeSuspendCall
import com.cbgm.sparrow.core.util.time.SystemClock
import com.cbgm.sparrow.feature.attachments.domain.repository.MessageAttachmentOperationsRepository
import com.cbgm.sparrow.feature.attachments.domain.repository.MessageAttachmentRepository
import kotlinx.coroutines.flow.first

/**
 * Builds the revision of an edited expense. Only the creator or an admin may edit a live expense of the open board.
 * A receipt that an earlier revision already carries gets a new ID, so deleting the old revision's receipt keeps it.
 */
class PrepareGroupExpenseRevisionUseCase(
    private val findGroupExpenseMembers: FindGroupExpenseMembersUseCase,
    private val attachmentOperations: MessageAttachmentOperationsRepository,
    private val attachmentRepository: MessageAttachmentRepository
) {
    suspend operator fun invoke(groupId: String, expense: Expense): Result<Expense> =
        safeSuspendCall(logFailure = false) {
            val members = findGroupExpenseMembers(groupId).getOrThrow()
            members.requireCurrentBoard(expense.boardId)
            check(expense.deletedAtEpochMilliseconds == null) { "A deleted expense cannot be edited" }
            check(members.canChange(expense)) { "Only the creator or an admin can edit this expense" }
            members.requireMembers(expense.paidByMemberId, expense.allocations)
            val revised = expense.copy(
                id = IdGenerator.generate(prefix = "expense"),
                revisionOf = expense.expenseId,
                allocations = expense.allocations.sortedBy { it.memberId },
                receipt = expense.receipt?.let { receipt -> receiptForRevision(groupId, receipt) },
                editedAtEpochMilliseconds = SystemClock.nowEpochMilliseconds()
            )
            ExpensePolicy.requireValid(revised)
            revised
        }

    private suspend fun receiptForRevision(groupId: String, receipt: Image): Image {
        val stored = attachmentOperations.observeExpenses(groupId).first()
        if (stored.none { it.receipt?.id == receipt.id }) return receipt
        val localFilePath =
            receipt.localFilePath ?: attachmentRepository.loadLocalFile(receipt.id, groupId).getOrThrow()
        return receipt.copy(id = IdGenerator.generate(prefix = RECEIPT_ID_PREFIX), localFilePath = localFilePath)
    }

    private companion object {
        const val RECEIPT_ID_PREFIX = "expense-receipt"
    }
}
