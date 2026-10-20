package com.cbgm.sparrow.feature.expenses.domain

import com.cbgm.sparrow.core.base.messagepart.domain.model.Expense
import com.cbgm.sparrow.core.base.messagepart.domain.model.ExpenseAllocation
import kotlin.test.Test
import kotlin.test.assertEquals

class ExpenseRevisionsTest {
    private val taxi = Expense("taxi", "board", "Taxi", 500L, "EUR", "bob", listOf(ExpenseAllocation("bob", 500L)), 1L)
    private val dinner = Expense("dinner", "board", "Dinner", 900L, "EUR", "bob", listOf(ExpenseAllocation("bob", 900L)), 1L)

    @Test
    fun newestRevisionWinsRegardlessOfOrder() {
        val edited = taxi.copy(amountMinor = 700L, allocations = listOf(ExpenseAllocation("bob", 700L)), editedAtEpochMilliseconds = 5L, id = "taxi-2", revisionOf = "taxi")

        assertEquals(listOf(edited), ExpenseRevisions.current(listOf(edited, taxi)))
        assertEquals(listOf(edited), ExpenseRevisions.current(listOf(taxi, edited)))
    }

    @Test
    fun deletedExpensesAreDropped() {
        val deleted = taxi.copy(id = "taxi-3", revisionOf = "taxi", deletedAtEpochMilliseconds = 6L)

        assertEquals(listOf(dinner), ExpenseRevisions.current(listOf(taxi, dinner, deleted)))
    }
}
