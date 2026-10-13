package com.cbgm.sparrow.feature.expenses.domain.model

import com.cbgm.sparrow.core.messagepart.domain.model.Expense
import com.cbgm.sparrow.core.messagepart.domain.model.ExpenseAllocation
import com.cbgm.sparrow.core.messagepart.domain.model.ExpenseCategory

sealed interface ExpenseRequest {
    val groupId: String

    data class Create(
        override val groupId: String,
        val boardId: String,
        val description: String,
        val amountMinor: Long,
        val paidByMemberId: String,
        val allocations: List<ExpenseAllocation>,
        val category: ExpenseCategory
    ) : ExpenseRequest

    data class Edit(
        override val groupId: String,
        val expense: Expense
    ) : ExpenseRequest

    data class Delete(
        override val groupId: String,
        val expense: Expense
    ) : ExpenseRequest
}
