package com.cbgm.sparrow.feature.expenses.presentation.pin.mapper

import com.cbgm.sparrow.feature.expenses.domain.model.ExpenseSummary
import com.cbgm.sparrow.feature.expenses.presentation.pin.model.ExpensePinBalanceUi
import com.cbgm.sparrow.feature.expenses.presentation.pin.model.ExpensePinUi
import kotlin.test.Test
import kotlin.test.assertEquals

class ExpensePinUiMapperTest {
    private val summary = ExpenseSummary("board", "EUR", 5, 108650L, -16880L)

    @Test
    fun mapsDebtToOwes() {
        assertEquals(
            ExpensePinUi(5, "€1,086.50", ExpensePinBalanceUi.Owes("€168.80")),
            summary.toExpensePinUi()
        )
    }

    @Test
    fun mapsCreditSettledAndUnavailable() {
        assertEquals(ExpensePinBalanceUi.Owed("€432.70"), summary.copy(localBalanceMinor = 43270L).toExpensePinUi().balance)
        assertEquals(ExpensePinBalanceUi.Settled, summary.copy(localBalanceMinor = 0L).toExpensePinUi().balance)
        assertEquals(ExpensePinBalanceUi.Unavailable, summary.copy(localBalanceMinor = null).toExpensePinUi().balance)
    }
}
