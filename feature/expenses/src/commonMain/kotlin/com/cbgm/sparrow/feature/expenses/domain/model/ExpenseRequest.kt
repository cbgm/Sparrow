package com.cbgm.sparrow.feature.expenses.domain.model

import com.cbgm.sparrow.core.base.messagepart.domain.model.Expense
import com.cbgm.sparrow.core.base.messagepart.domain.model.ExpenseAllocation
import com.cbgm.sparrow.core.base.messagepart.domain.model.ExpenseCategory
import com.cbgm.sparrow.core.base.messagepart.domain.model.ExpenseConversion
import com.cbgm.sparrow.core.base.messagepart.domain.model.Image

sealed interface ExpenseRequest {
    val groupId: String

    data class Create(
        override val groupId: String,
        val boardId: String,
        val description: String,
        val amountMinor: Long,
        val paidByMemberId: String,
        val allocations: List<ExpenseAllocation>,
        val category: ExpenseCategory,
        val receipt: Image? = null,
        val conversion: ExpenseConversion? = null
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
