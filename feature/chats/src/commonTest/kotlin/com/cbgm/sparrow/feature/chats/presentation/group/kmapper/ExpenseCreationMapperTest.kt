package com.cbgm.sparrow.feature.chats.presentation.group.kmapper

import com.cbgm.sparrow.core.messagepart.domain.model.Expense
import com.cbgm.sparrow.core.messagepart.domain.model.ExpenseAllocation
import com.cbgm.sparrow.core.messagepart.domain.model.ExpenseBoard
import com.cbgm.sparrow.feature.expenses.domain.model.ExpenseBalance
import com.cbgm.sparrow.feature.expenses.domain.usecase.CalculateExpenseBalancesUseCase
import com.cbgm.sparrow.feature.expenses.presentation.create.model.ExpenseDraftUi
import com.cbgm.sparrow.feature.expenses.presentation.create.model.ExpenseSplitModeUi
import kotlin.test.Test
import kotlin.test.assertEquals

class ExpenseCreationMapperTest {
    @Test
    fun customSplitReducesDebtByOthersShare() {
        val board = ExpenseBoard("board", "EUR", 1L)
        // Chris paid 12 € split equally: Bob owes Chris 6 €.
        val dinner = Expense("dinner", "board", "Dinner", 1200L, "EUR", "chris", listOf(ExpenseAllocation("bob", 600L), ExpenseAllocation("chris", 600L)), 2L)
        // Bob pays 5 €: 0,50 € for himself, 4,50 € for Chris.
        val draft = ExpenseDraftUi("Taxi", "5", "bob", setOf("bob", "chris"))
            .withSplitMode(ExpenseSplitModeUi.CUSTOM)
            .copy(customAmounts = mapOf("bob" to "0,50", "chris" to "4,50"))
        val allocations = draft.toExpenseAllocations()!!
        val taxi = Expense("taxi", "board", "Taxi", 500L, "EUR", "bob", allocations, 3L)

        val balances = CalculateExpenseBalancesUseCase()(board, listOf(dinner, taxi)).getOrThrow()

        assertEquals(listOf(ExpenseBalance("bob", -150L), ExpenseBalance("chris", 150L)), balances)
    }
}
