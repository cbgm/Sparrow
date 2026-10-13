package com.cbgm.sparrow.feature.chats.presentation.group.mapper

import com.cbgm.sparrow.feature.chats.domain.model.group.GroupExpenseSummary
import com.cbgm.sparrow.feature.expenses.presentation.common.mapper.toExpenseAmountLabel
import com.cbgm.sparrow.feature.expenses.presentation.pin.model.ExpensePinBalanceUi
import com.cbgm.sparrow.feature.expenses.presentation.pin.model.ExpensePinUi

internal fun GroupExpenseSummary.toExpensePinUi(): ExpensePinUi =
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
