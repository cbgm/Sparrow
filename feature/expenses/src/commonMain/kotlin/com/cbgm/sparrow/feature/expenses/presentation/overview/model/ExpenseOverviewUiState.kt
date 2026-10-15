package com.cbgm.sparrow.feature.expenses.presentation.overview.model

import com.cbgm.sparrow.feature.expenses.presentation.create.model.ExpenseCategoryUi
import com.cbgm.sparrow.feature.expenses.presentation.create.model.ExpenseParticipantUi

data class ExpenseOverviewUiState(
    val isLoading: Boolean = true,
    val currencyCode: String = "",
    val activatedAtLabel: String = "",
    val yourBalance: ExpenseYourBalanceUi = ExpenseYourBalanceUi(),
    val settlements: List<ExpenseSettlementUi> = emptyList(),
    val balances: List<ExpenseMemberBalanceUi> = emptyList(),
    val expenses: List<ExpenseListItemUi> = emptyList()
)

data class ExpenseYourBalanceUi(
    val balance: ExpenseAmountUi = ExpenseAmountUi.Settled(""),
    val totalLabel: String = "",
    val yourShareLabel: String = "",
    val youPaidLabel: String = "",
    val counterpartCount: Int = 0
)

sealed interface ExpenseAmountUi {
    data class Owes(
        val amountLabel: String
    ) : ExpenseAmountUi

    data class Owed(
        val amountLabel: String
    ) : ExpenseAmountUi

    data class Settled(
        val amountLabel: String
    ) : ExpenseAmountUi
}

data class ExpenseSettlementUi(
    val from: ExpenseParticipantUi,
    val to: ExpenseParticipantUi,
    val amountLabel: String
)

data class ExpenseMemberBalanceUi(
    val member: ExpenseParticipantUi,
    val balance: ExpenseAmountUi,
    val fraction: Float
)

data class ExpenseListItemUi(
    val id: String,
    val description: String,
    val category: ExpenseCategoryUi,
    val payer: ExpenseParticipantUi,
    val dateLabel: String,
    val amountLabel: String,
    val yourPart: ExpenseYourPartUi,
    val canChange: Boolean = false
)

sealed interface ExpenseYourPartUi {
    data class Lent(
        val amountLabel: String
    ) : ExpenseYourPartUi

    data class Borrowed(
        val amountLabel: String
    ) : ExpenseYourPartUi

    data object NotInvolved : ExpenseYourPartUi
}
