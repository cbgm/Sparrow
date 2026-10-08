package com.cbgm.sparrow.feature.expenses.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class ExpenseMoneyTest {
    @Test
    fun parsesExactAmountsWithoutFloatingPoint() {
        assertEquals(123L, ExpenseMoney.parseEuroCents("1.23"))
        assertEquals(120L, ExpenseMoney.parseEuroCents("1,2"))
        assertEquals(1L, ExpenseMoney.parseEuroCents("0.01"))
        assertNull(ExpenseMoney.parseEuroCents("0"))
        assertNull(ExpenseMoney.parseEuroCents("12.345"))
        assertNull(ExpenseMoney.parseEuroCents("92233720368547758"))
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
