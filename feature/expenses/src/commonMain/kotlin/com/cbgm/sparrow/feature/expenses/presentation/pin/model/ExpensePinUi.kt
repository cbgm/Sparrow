package com.cbgm.sparrow.feature.expenses.presentation.pin.model

data class ExpensePinUi(
    val expenseCount: Int,
    val totalLabel: String,
    val balance: ExpensePinBalanceUi
)

sealed interface ExpensePinBalanceUi {
    data class Owes(
        val amountLabel: String
    ) : ExpensePinBalanceUi

    data class Owed(
        val amountLabel: String
    ) : ExpensePinBalanceUi

    data object Settled : ExpensePinBalanceUi

    data object Unavailable : ExpensePinBalanceUi
}
