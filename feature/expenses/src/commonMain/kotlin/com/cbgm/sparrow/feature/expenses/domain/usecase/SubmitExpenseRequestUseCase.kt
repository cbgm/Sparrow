package com.cbgm.sparrow.feature.expenses.domain.usecase

import com.cbgm.sparrow.feature.expenses.domain.model.ExpenseRequest
import com.cbgm.sparrow.feature.expenses.domain.repository.ExpenseComposerRepository

class SubmitExpenseRequestUseCase(
    private val repository: ExpenseComposerRepository
) {
    suspend operator fun invoke(request: ExpenseRequest): Result<Unit> = repository.submit(request)
}
