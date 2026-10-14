package com.cbgm.sparrow.feature.expenses.presentation.detail.model

import com.cbgm.sparrow.feature.expenses.presentation.create.model.ExpenseCategoryUi
import com.cbgm.sparrow.feature.expenses.presentation.create.model.ExpenseParticipantUi

data class ExpenseDetailUiState(
    val isLoading: Boolean = true,
    val description: String = "",
    val amountLabel: String = "",
    val category: ExpenseCategoryUi = ExpenseCategoryUi.OTHER,
    val payer: ExpenseParticipantUi = ExpenseParticipantUi("", ""),
    val dateLabel: String = "",
    val shares: List<ExpenseShareUi> = emptyList(),
    val equalShareLabel: String? = null,
    val receiptPath: String? = null,
    val canChange: Boolean = false
)

data class ExpenseShareUi(
    val member: ExpenseParticipantUi,
    val amountLabel: String
)
