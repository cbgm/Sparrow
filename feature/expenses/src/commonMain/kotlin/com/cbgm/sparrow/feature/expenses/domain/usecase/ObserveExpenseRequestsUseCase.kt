package com.cbgm.sparrow.feature.expenses.domain.usecase

import com.cbgm.sparrow.feature.expenses.domain.model.ExpenseRequest
import com.cbgm.sparrow.feature.expenses.domain.repository.ExpenseComposerRepository
import kotlinx.coroutines.flow.Flow

class ObserveExpenseRequestsUseCase(
    private val repository: ExpenseComposerRepository
) {
    operator fun invoke(): Flow<ExpenseRequest> = repository.observeRequests()
}
