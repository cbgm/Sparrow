package com.cbgm.sparrow.feature.expenses.presentation.overview.mapper

import com.cbgm.sparrow.core.messagepart.domain.model.Expense
import com.cbgm.sparrow.core.messagepart.domain.model.ExpenseAllocation
import com.cbgm.sparrow.core.messagepart.domain.model.ExpenseBoard
import com.cbgm.sparrow.feature.expenses.domain.model.ExpenseMember
import com.cbgm.sparrow.feature.expenses.domain.model.ExpenseOverview
import com.cbgm.sparrow.feature.expenses.domain.usecase.CalculateExpenseBalancesUseCase
import com.cbgm.sparrow.feature.expenses.domain.usecase.CalculateExpenseSettlementsUseCase
import com.cbgm.sparrow.feature.expenses.presentation.detail.mapper.toExpenseDetailUiState
import com.cbgm.sparrow.feature.expenses.presentation.overview.model.ExpenseAmountUi
import com.cbgm.sparrow.feature.expenses.presentation.overview.model.ExpenseYourPartUi
import kotlin.test.Test
import kotlin.test.assertEquals

class ExpenseOverviewUiMapperTest {
    private val board = ExpenseBoard("board", "EUR", 1L)
    private val members = listOf(
        ExpenseMember("chris", "Chris", isLocal = true),
        ExpenseMember("bob", "Bob", isLocal = false, avatarContactId = "contact-bob")
    )
    private val dinner = Expense(
        "dinner",
        "board",
        "Dinner",
        1200L,
        "EUR",
        "chris",
        listOf(ExpenseAllocation("bob", 600L), ExpenseAllocation("chris", 600L)),
        2L,
        createdByMemberId = "chris"
    )
    private val taxi = Expense(
        "taxi",
        "board",
        "Taxi",
        500L,
        "EUR",
        "bob",
        listOf(ExpenseAllocation("bob", 50L), ExpenseAllocation("chris", 450L)),
        3L,
        createdByMemberId = "bob"
    )

    @Test
    fun mapsBalancesSettlementsAndYourPart() {
        val expenses = listOf(taxi, dinner)
        val balances = CalculateExpenseBalancesUseCase()(board, expenses).getOrThrow()
        val overview = ExpenseOverview(
            board = board,
            localMemberId = "chris",
            isLocalAdmin = false,
            members = members,
            expenses = expenses,
            balances = balances,
            settlements = CalculateExpenseSettlementsUseCase()(balances).getOrThrow()
        )

        val state = overview.toExpenseOverviewUiState()

        assertEquals(ExpenseAmountUi.Owed("€1.50"), state.yourBalance.balance)
        assertEquals("€17.00", state.yourBalance.totalLabel)
        assertEquals("€10.50", state.yourBalance.yourShareLabel)
        assertEquals("€12.00", state.yourBalance.youPaidLabel)
        assertEquals(1, state.yourBalance.counterpartCount)
        assertEquals("bob", state.settlements.single().from.id)
        assertEquals("€1.50", state.settlements.single().amountLabel)
        assertEquals(listOf("chris", "bob"), state.balances.map { it.member.id })
        assertEquals(ExpenseYourPartUi.Borrowed("€4.50"), state.expenses[0].yourPart)
        assertEquals(ExpenseYourPartUi.Lent("€6.00"), state.expenses[1].yourPart)
        assertEquals(listOf(false, true), state.expenses.map { it.canChange })
        assertEquals(listOf(true, true), overview.copy(isLocalAdmin = true).toExpenseOverviewUiState().expenses.map { it.canChange })
    }

    @Test
    fun mapsDetailWithCustomSplitAndLocalShareFirst() {
        val overview = ExpenseOverview(
            board = board,
            localMemberId = "chris",
            isLocalAdmin = false,
            members = members,
            expenses = listOf(taxi, dinner),
            balances = emptyList(),
            settlements = emptyList()
        )

        val taxiDetail = overview.toExpenseDetailUiState(taxi)
        val dinnerDetail = overview.toExpenseDetailUiState(dinner)

        assertEquals("€5.00", taxiDetail.amountLabel)
        assertEquals("bob", taxiDetail.payer.id)
        assertEquals(listOf("chris" to "€4.50", "bob" to "€0.50"), taxiDetail.shares.map { it.member.id to it.amountLabel })
        assertEquals(null, taxiDetail.equalShareLabel)
        assertEquals(false, taxiDetail.canChange)
        assertEquals("€6.00", dinnerDetail.equalShareLabel)
        assertEquals(true, dinnerDetail.canChange)
    }
}
