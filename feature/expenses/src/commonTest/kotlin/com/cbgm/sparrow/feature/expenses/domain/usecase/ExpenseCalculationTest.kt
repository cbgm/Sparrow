package com.cbgm.sparrow.feature.expenses.domain.usecase

import com.cbgm.sparrow.core.messagepart.domain.model.Expense
import com.cbgm.sparrow.core.messagepart.domain.model.ExpenseAllocation
import com.cbgm.sparrow.core.messagepart.domain.model.ExpenseBoard
import com.cbgm.sparrow.feature.expenses.domain.model.ExpenseBalance
import com.cbgm.sparrow.feature.expenses.domain.model.ExpenseSettlementSuggestion
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ExpenseCalculationTest {
    private val board = ExpenseBoard("board-1", "EUR", 1L)
    private val expenses = listOf(
        Expense(
            "expense-1",
            "board-1",
            "Hotel",
            9000L,
            "EUR",
            "alex",
            listOf(ExpenseAllocation("alex", 3000L), ExpenseAllocation("ben", 3000L), ExpenseAllocation("chris", 3000L)),
            2L
        ),
        Expense(
            "expense-2",
            "board-1",
            "Dinner",
            3000L,
            "EUR",
            "ben",
            listOf(ExpenseAllocation("alex", 1000L), ExpenseAllocation("ben", 1000L), ExpenseAllocation("chris", 1000L)),
            3L
        )
    )

    @Test fun calculatesBalancesAndTransfers() {
        val balances = CalculateExpenseBalancesUseCase()(board, expenses).getOrThrow()
        assertEquals(
            listOf(
                ExpenseBalance("alex", 5000L),
                ExpenseBalance("ben", -1000L),
                ExpenseBalance("chris", -4000L)
            ),
            balances
        )
        assertEquals(
            listOf(
                ExpenseSettlementSuggestion("ben", "alex", 1000L),
                ExpenseSettlementSuggestion("chris", "alex", 4000L)
            ),
            CalculateExpenseSettlementsUseCase()(balances).getOrThrow()
        )
    }

    @Test fun rejectsExpenseFromOtherBoard() {
        val result = CalculateExpenseBalancesUseCase()(board, listOf(expenses[0].copy(boardId = "another")))
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
    }

    @Test fun rejectsUnbalancedInput() {
        val result = CalculateExpenseSettlementsUseCase()(listOf(ExpenseBalance("alex", 42L)))
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
    }

    @Test fun rejectsUnrepresentableDebts() {
        val result = CalculateExpenseSettlementsUseCase()(
            listOf(ExpenseBalance("alex", Long.MIN_VALUE), ExpenseBalance("ben", Long.MAX_VALUE))
        )
        assertTrue(result.isFailure)
    }

    @Test fun supportsAlreadySettledBalances() {
        val balances = listOf(ExpenseBalance("alex", 0L), ExpenseBalance("ben", 0L))
        assertEquals(emptyList(), CalculateExpenseSettlementsUseCase()(balances).getOrThrow())
    }
}
