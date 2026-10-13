package com.cbgm.sparrow.feature.expenses.domain.usecase

import com.cbgm.sparrow.feature.expenses.domain.repository.ExpenseComposerRepository

class ClearExpenseGroupUseCase(
    private val repository: ExpenseComposerRepository
) {
    operator fun invoke(groupId: String) = repository.clearGroup(groupId)
}
