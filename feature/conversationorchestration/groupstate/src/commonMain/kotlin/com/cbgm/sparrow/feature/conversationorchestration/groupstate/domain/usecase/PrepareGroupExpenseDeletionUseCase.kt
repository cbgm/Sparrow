package com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.usecase

import com.cbgm.sparrow.core.base.messagepart.domain.model.Expense
import com.cbgm.sparrow.core.base.messagepart.domain.model.ExpensePolicy
import com.cbgm.sparrow.core.base.messagepart.domain.model.expenseId
import com.cbgm.sparrow.core.util.id.IdGenerator
import com.cbgm.sparrow.core.util.result.safeSuspendCall
import com.cbgm.sparrow.core.util.time.SystemClock

/**
 * Builds the deletion revision (tombstone) of an expense. Only the creator or an admin may delete a live expense
 * of the open board; the tombstone drops the receipt.
 */
class PrepareGroupExpenseDeletionUseCase(
    private val findGroupExpenseMembers: FindGroupExpenseMembersUseCase
) {
    suspend operator fun invoke(groupId: String, expense: Expense): Result<Expense> =
        safeSuspendCall(logFailure = false) {
            val members = findGroupExpenseMembers(groupId).getOrThrow()
            members.requireCurrentBoard(expense.boardId)
            check(expense.deletedAtEpochMilliseconds == null) { "Expense is already deleted" }
            check(members.canChange(expense)) { "Only the creator or an admin can delete this expense" }
            val deleted = expense.copy(
                id = IdGenerator.generate(prefix = "expense"),
                revisionOf = expense.expenseId,
                receipt = null,
                editedAtEpochMilliseconds = null,
                deletedAtEpochMilliseconds = SystemClock.nowEpochMilliseconds()
            )
            ExpensePolicy.requireValid(deleted)
            deleted
        }
}
