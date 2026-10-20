package com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.policy

import com.cbgm.sparrow.core.base.messagepart.domain.model.Expense
import com.cbgm.sparrow.core.base.messagepart.domain.model.expenseId

object IncomingExpensePolicy {
    fun requireAuthorized(
        expense: Expense,
        storedRevisions: List<Expense>,
        senderMemberId: String,
        senderIsAdmin: Boolean
    ) {
        require(storedRevisions.none { it.id == expense.id }) { "Expense part ID already exists" }
        val original = storedRevisions.firstOrNull { it.id == expense.expenseId }
        if (expense.revisionOf == null) {
            require(senderIsAdmin || expense.createdByMemberId == senderMemberId) {
                "Expense creator does not match its sender"
            }
        } else {
            requireNotNull(original) { "Expense revision has no original" }
            require(expense.createdByMemberId == original.createdByMemberId) { "Expense creator cannot change" }
            require(senderIsAdmin || original.createdByMemberId == senderMemberId) {
                "Only the creator or an admin can change an expense"
            }
        }
    }
}
