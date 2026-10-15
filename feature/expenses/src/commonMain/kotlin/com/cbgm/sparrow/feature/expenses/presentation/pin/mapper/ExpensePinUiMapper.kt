package com.cbgm.sparrow.feature.expenses.presentation.pin.mapper

import com.cbgm.sparrow.feature.expenses.domain.model.ExpenseSummary
import com.cbgm.sparrow.feature.expenses.presentation.common.mapper.toExpenseAmountLabel
import com.cbgm.sparrow.feature.expenses.presentation.pin.model.ExpensePinBalanceUi
import com.cbgm.sparrow.feature.expenses.presentation.pin.model.ExpensePinUi

fun ExpenseSummary.toExpensePinUi(): ExpensePinUi =
    ExpensePinUi(
        expenseCount = expenseCount,
        totalLabel = totalMinor.toExpenseAmountLabel(currencyCode),
        balance = when {
            localBalanceMinor == null -> ExpensePinBalanceUi.Unavailable
            localBalanceMinor < 0L -> ExpensePinBalanceUi.Owes((-localBalanceMinor).toExpenseAmountLabel(currencyCode))
            localBalanceMinor > 0L -> ExpensePinBalanceUi.Owed(localBalanceMinor.toExpenseAmountLabel(currencyCode))
            else -> ExpensePinBalanceUi.Settled
        }
    )
