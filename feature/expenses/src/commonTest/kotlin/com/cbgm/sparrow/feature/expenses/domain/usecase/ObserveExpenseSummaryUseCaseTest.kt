package com.cbgm.sparrow.feature.expenses.domain.usecase

import com.cbgm.sparrow.core.messagepart.domain.model.Expense
import com.cbgm.sparrow.core.messagepart.domain.model.ExpenseAllocation
import com.cbgm.sparrow.core.messagepart.domain.model.ExpenseBoard
import com.cbgm.sparrow.core.messagepart.domain.model.ExpenseMemberId
import com.cbgm.sparrow.feature.expenses.data.repository.ExpenseComposerRepositoryImpl
import com.cbgm.sparrow.feature.expenses.domain.model.ExpenseGroup
import com.cbgm.sparrow.feature.expenses.domain.model.ExpenseSummary
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ObserveExpenseSummaryUseCaseTest {
    private val localKey = byteArrayOf(1, 2, 3)
    private val localId = ExpenseMemberId.fromSigningPublicKey(localKey)
    private val board = ExpenseBoard("board-1", "EUR", 1L)

    @Test
    fun summarizesActiveBoardForLocalMember() = runTest {
        val expenses = listOf(
            expense("hotel", "alex", 9000L, listOf("alex", localId, "mia")),
            expense("dinner", localId, 3000L, listOf("alex", localId, "mia"))
        )

        val summary = useCase(board, expenses)("group").first()

        assertEquals(
            ExpenseSummary(
                boardId = "board-1",
                currencyCode = "EUR",
                expenseCount = 2,
                totalMinor = 12000L,
                localBalanceMinor = -1000L
            ),
            summary
        )
    }

    @Test
    fun emitsNullWithoutActiveBoard() = runTest {
        assertNull(useCase(null, emptyList())("group").first())
    }

    @Test
    fun reportsSettledWhenLocalMemberHasNoShare() = runTest {
        val summary = useCase(board, listOf(expense("taxi", "alex", 1000L, listOf("alex", "mia"))))("group").first()

        assertEquals(0L, summary?.localBalanceMinor)
    }

    private fun useCase(board: ExpenseBoard?, expenses: List<Expense>): ObserveExpenseSummaryUseCase {
        val repository = ExpenseComposerRepositoryImpl()
        val group = board?.let { ExpenseGroup("group", it, localId, false, emptyList(), expenses) }
        repository.publishGroup("group", group)
        return ObserveExpenseSummaryUseCase(repository, CalculateExpenseBalancesUseCase())
    }

    private fun expense(
        id: String,
        payer: String,
        amount: Long,
        members: List<String>
    ) = Expense(
        id = id,
        boardId = "board-1",
        description = id,
        amountMinor = amount,
        currencyCode = "EUR",
        paidByMemberId = payer,
        allocations = members.map { ExpenseAllocation(it, amount / members.size) },
        occurredAtEpochMilliseconds = 2L
    )
}
