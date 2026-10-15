package com.cbgm.sparrow.feature.expenses.presentation.common.mapper

import kotlin.test.Test
import kotlin.test.assertEquals

class ExpenseAmountMapperTest {
    @Test
    fun formatsEuroWithGrouping() {
        assertEquals("€0.05", 5L.toExpenseAmountLabel("EUR"))
        assertEquals("€650.00", 65000L.toExpenseAmountLabel("EUR"))
        assertEquals("€1,086.50", 108650L.toExpenseAmountLabel("EUR"))
        assertEquals("€1,234,567.89", 123456789L.toExpenseAmountLabel("EUR"))
    }

    @Test
    fun formatsNegativeAndOtherCurrencies() {
        assertEquals("-€168.80", (-16880L).toExpenseAmountLabel("EUR"))
        assertEquals("$12.30", 1230L.toExpenseAmountLabel("USD"))
        assertEquals("12.30 CHF", 1230L.toExpenseAmountLabel("CHF"))
        assertEquals("¥1,500", 1500L.toExpenseAmountLabel("JPY"))
    }
}
