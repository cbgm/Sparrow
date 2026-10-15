package com.cbgm.sparrow.feature.expenses.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class ExpenseMoneyTest {
    @Test
    fun parsesExactAmountsWithoutFloatingPoint() {
        assertEquals(123L, ExpenseMoney.parseMinor("1.23", "EUR"))
        assertEquals(120L, ExpenseMoney.parseMinor("1,2", "EUR"))
        assertEquals(1L, ExpenseMoney.parseMinor("0.01", "EUR"))
        assertNull(ExpenseMoney.parseMinor("0", "EUR"))
        assertNull(ExpenseMoney.parseMinor("12.345", "EUR"))
        assertNull(ExpenseMoney.parseMinor("92233720368547758", "EUR"))
    }

    @Test
    fun parsesCurrenciesWithOtherFractionDigits() {
        assertEquals(1_500L, ExpenseMoney.parseMinor("1500", "JPY"))
        assertNull(ExpenseMoney.parseMinor("1500.5", "JPY"))
        assertEquals(12_345L, ExpenseMoney.parseMinor("12.345", "KWD"))
        assertEquals("1500", ExpenseMoney.toInput(1_500L, "JPY"))
        assertEquals("12,345", ExpenseMoney.toInput(12_345L, "KWD", ','))
    }

    @Test
    fun convertsBetweenCurrenciesAndRoundsToMinorUnits() {
        assertEquals(4_465L, ExpenseMoney.convert(5_000L, "USD", "EUR", "0.89294"))
        assertEquals(9_524L, ExpenseMoney.convert(15_000L, "JPY", "EUR", "0.00634957"))
        assertEquals(15_749L, ExpenseMoney.convert(10_000L, "EUR", "JPY", "157.49"))
    }

    @Test
    fun convertedSharesAlwaysAddUpToTheConvertedTotal() {
        val shares = mapOf("a" to 1_001L, "b" to 1_001L, "c" to 998L)
        val total = ExpenseMoney.convert(3_000L, "USD", "EUR", "0.89294")

        val converted = ExpenseMoney.convertShares(shares, total, "USD", "EUR", "0.89294")!!

        assertEquals(total, converted.values.sum())
    }

    @Test
    fun formatsRatesWithoutScientificNotation() {
        assertEquals("0.89294", ExpenseMoney.formatRate(0.89294))
        assertEquals("157.49", ExpenseMoney.formatRate(157.49))
        assertEquals("0.000012345678", ExpenseMoney.formatRate(0.000012345678))
        assertEquals("1", ExpenseMoney.formatRate(1.0))
    }

    @Test
    fun distributesRemainderDeterministically() {
        assertEquals(
            mapOf("a" to 34L, "b" to 33L, "c" to 33L),
            ExpenseMoney.splitEvenly(100L, setOf("c", "a", "b"))
        )
    }

    @Test
    fun rejectsSplitsWithUnrepresentableShares() {
        assertFailsWith<IllegalArgumentException> {
            ExpenseMoney.splitEvenly(1L, setOf("a", "b"))
        }
    }
}
