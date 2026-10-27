package com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.model

import com.cbgm.sparrow.core.base.messagepart.domain.model.Expense
import com.cbgm.sparrow.core.base.messagepart.domain.model.ExpenseAllocation
import com.cbgm.sparrow.core.base.messagepart.domain.model.ExpenseBoard
import com.cbgm.sparrow.core.base.messagepart.domain.model.ExpensePolicy

/** The open expense board and the expense member IDs that may currently take part in it. */
data class GroupExpenseMembers(
    val board: ExpenseBoard,
    val memberIds: Set<String>,
    val localMemberId: String,
    val isLocalAdmin: Boolean
) {
    /** An expense that refers to another board would land on a board nobody sees any more. */
    fun requireCurrentBoard(boardId: String) {
        check(board.id == boardId) { "Expense board has changed" }
    }

    /** Former members cannot pay or share an expense because they no longer settle the board. */
    fun requireMembers(paidByMemberId: String, allocations: List<ExpenseAllocation>) {
        check(paidByMemberId in memberIds) { "Payer is no longer a group member" }
        check(allocations.all { it.memberId in memberIds }) { "Expense includes a former group member" }
    }

    /** Whether the local member may edit or delete [expense]. */
    fun canChange(expense: Expense): Boolean =
        ExpensePolicy.canChange(expense, memberId = localMemberId, isAdmin = isLocalAdmin)
}
