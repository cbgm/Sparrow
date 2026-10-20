package com.cbgm.sparrow.feature.expenses.presentation.overview.model

sealed interface ExpenseOverviewUiEvent {
    data class PaymentClicked(
        val fromMemberId: String,
        val toMemberId: String,
        val amountMinor: Long,
        val transferId: String? = null
    ) : ExpenseOverviewUiEvent

    data object BackClicked : ExpenseOverviewUiEvent

    data class ExpenseClicked(
        val expenseId: String
    ) : ExpenseOverviewUiEvent

    data class EditExpenseClicked(
        val expenseId: String
    ) : ExpenseOverviewUiEvent

    data class DeleteExpenseConfirmed(
        val expenseId: String
    ) : ExpenseOverviewUiEvent
}
