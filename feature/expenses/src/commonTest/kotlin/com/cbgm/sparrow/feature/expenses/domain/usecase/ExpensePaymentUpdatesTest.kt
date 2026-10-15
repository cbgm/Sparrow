package com.cbgm.sparrow.feature.expenses.domain.usecase

import com.cbgm.sparrow.core.messagepart.domain.model.Expense
import com.cbgm.sparrow.core.messagepart.domain.model.ExpenseAllocation
import com.cbgm.sparrow.core.messagepart.domain.model.ExpenseBoard
import com.cbgm.sparrow.core.messagepart.domain.model.PaymentTransfer
import com.cbgm.sparrow.core.messagepart.domain.model.PaymentTransferStatus
import com.cbgm.sparrow.feature.expenses.data.repository.ExpenseComposerRepositoryImpl
import com.cbgm.sparrow.feature.expenses.domain.model.ExpenseGroup
import com.cbgm.sparrow.feature.expenses.domain.model.ExpenseMember
import com.cbgm.sparrow.feature.expenses.domain.model.ExpenseOverview
import com.cbgm.sparrow.feature.expenses.domain.model.ExpenseSummary
import com.cbgm.sparrow.feature.expenses.presentation.overview.mapper.toExpenseOverviewUiState
import com.cbgm.sparrow.feature.expenses.presentation.overview.model.ExpenseAmountUi
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ExpensePaymentUpdatesTest {
    private val expense = Expense(
        "expense",
        "board",
        "Dinner",
        10000L,
        "EUR",
        "recipient",
        listOf(ExpenseAllocation("payer", 5000L), ExpenseAllocation("recipient", 5000L)),
        1L,
        createdByMemberId = "recipient"
    )
    private val board = ExpenseBoard("board", "EUR", 1L)
    private val members = listOf(ExpenseMember("payer", "Payer", true), ExpenseMember("recipient", "Recipient", false))
    private val sent = PaymentTransfer(
        "payment-sent",
        "payment",
        "board",
        "payer",
        "recipient",
        5000L,
        "EUR",
        "SPARROW-test",
        PaymentTransferStatus.SENT,
        2L
    )
    private val receipt = sent.copy(id = "payment-received", status = PaymentTransferStatus.RECEIVED)

    @Test
    fun receiptUpdatesOverviewPaidAmountsBalancesHistoryAndPinFromTheSameBoard() = runTest {
        val repository = ExpenseComposerRepositoryImpl()
        val calculateBalances = CalculateExpenseBalancesUseCase()
        val observeOverview = ObserveExpenseOverviewUseCase(repository, calculateBalances, CalculateExpenseSettlementsUseCase())
        val observeSummary = ObserveExpenseSummaryUseCase(repository, calculateBalances)
        val initial = ExpenseGroup("group", board, "payer", false, members, listOf(expense))
        repository.publishGroup("group", initial).getOrThrow()
        val overviews = mutableListOf<ExpenseOverview>()
        val summaries = mutableListOf<ExpenseSummary?>()
        val overviewJob = launch(start = CoroutineStart.UNDISPATCHED) { observeOverview("group").take(4).toList(overviews) }
        val summaryJob = launch(start = CoroutineStart.UNDISPATCHED) { observeSummary("group").take(3).toList(summaries) }

        repository.publishGroup("group", initial.copy(transfers = listOf(sent))).getOrThrow()
        yield()
        val pending = overviews.last().toExpenseOverviewUiState()
        assertEquals(ExpenseAmountUi.Owes("€50.00"), pending.yourBalance.balance)
        assertEquals("€0.00", pending.yourBalance.youPaidLabel)
        assertFalse(pending.payments.single().isReceived)
        assertEquals(1, summaries.size)

        val confirmed = initial.copy(transfers = listOf(sent, receipt))
        repository.publishGroup("group", confirmed).getOrThrow()
        yield()
        val settled = overviews.last().toExpenseOverviewUiState()
        assertEquals(ExpenseAmountUi.Settled("€0.00"), settled.yourBalance.balance)
        assertEquals("€50.00", settled.yourBalance.youPaidLabel)
        assertEquals("€50.00", settled.yourBalance.yourShareLabel)
        assertEquals("€100.00", settled.yourBalance.totalLabel)
        assertTrue(settled.settlements.isEmpty())
        assertTrue(settled.balances.all { it.balance == ExpenseAmountUi.Settled("€0.00") })
        assertTrue(settled.payments.single().isReceived)
        assertEquals(0L, summaries.last()?.localBalanceMinor)
        assertEquals("€50.00", overviews.last().copy(localMemberId = "recipient").toExpenseOverviewUiState().yourBalance.youPaidLabel)

        val edit = expense.copy(
            id = "expense-edited",
            revisionOf = expense.id,
            amountMinor = 12000L,
            editedAtEpochMilliseconds = 3L,
            allocations = listOf(ExpenseAllocation("payer", 6000L), ExpenseAllocation("recipient", 6000L))
        )
        repository.publishGroup("group", confirmed.copy(expenses = listOf(expense, edit))).getOrThrow()
        overviewJob.join()
        summaryJob.join()
        val updated = overviews.last().toExpenseOverviewUiState()
        assertEquals(ExpenseAmountUi.Owes("€10.00"), updated.yourBalance.balance)
        assertEquals("€50.00", updated.yourBalance.youPaidLabel)
        assertEquals("€60.00", updated.yourBalance.yourShareLabel)
        assertEquals("€120.00", updated.yourBalance.totalLabel)
        assertEquals("€10.00", updated.settlements.single().amountLabel)
        assertEquals(-1000L, summaries.last()?.localBalanceMinor)
        assertEquals(12000L, summaries.last()?.totalMinor)
    }
}
