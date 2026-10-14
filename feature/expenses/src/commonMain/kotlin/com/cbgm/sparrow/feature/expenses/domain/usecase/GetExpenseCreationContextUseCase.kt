package com.cbgm.sparrow.feature.expenses.domain.usecase

import com.cbgm.sparrow.core.messagepart.domain.model.expenseId
import com.cbgm.sparrow.core.result.safeSuspendCall
import com.cbgm.sparrow.feature.expenses.domain.ExpenseRevisions
import com.cbgm.sparrow.feature.expenses.domain.model.ExpenseCreationContext
import com.cbgm.sparrow.feature.expenses.domain.repository.ExpenseComposerRepository
import kotlinx.coroutines.flow.first

class GetExpenseCreationContextUseCase(
    private val repository: ExpenseComposerRepository
) {
    suspend operator fun invoke(
        groupId: String,
        expenseId: String? = null
    ): Result<ExpenseCreationContext> = safeSuspendCall(logFailure = false) {
        require(groupId.isNotBlank()) { "Group ID is missing" }
        val group = repository.observeGroup(groupId).first() ?: error("Group expenses are not active")
        val localMemberId = group.localMemberId ?: error("Local expense member is missing")
        val expense = expenseId?.let { id ->
            ExpenseRevisions.current(group.expenses).firstOrNull { it.expenseId == id }
                ?: error("Expense was not found")
        }
        ExpenseCreationContext(group.board, localMemberId, group.members, expense, group.isLocalAdmin)
    }
}
