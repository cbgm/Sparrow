package com.cbgm.sparrow.feature.expenses.domain.usecase

import com.cbgm.sparrow.feature.expenses.domain.model.ExpenseGroup
import com.cbgm.sparrow.feature.expenses.domain.repository.ExpenseComposerRepository

class PublishExpenseGroupUseCase(
    private val repository: ExpenseComposerRepository
) {
    operator fun invoke(groupId: String, group: ExpenseGroup?): Result<Unit> =
        repository.publishGroup(groupId, group)
}
