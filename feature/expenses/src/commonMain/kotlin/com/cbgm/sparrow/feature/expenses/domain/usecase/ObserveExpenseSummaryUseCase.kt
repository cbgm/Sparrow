package com.cbgm.sparrow.feature.expenses.domain.usecase

import com.cbgm.sparrow.core.messagepart.domain.model.Expense
import com.cbgm.sparrow.feature.expenses.domain.ExpenseRevisions
import com.cbgm.sparrow.feature.expenses.domain.model.ExpenseGroup
import com.cbgm.sparrow.feature.expenses.domain.model.ExpenseSummary
import com.cbgm.sparrow.feature.expenses.domain.repository.ExpenseComposerRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

class ObserveExpenseSummaryUseCase(
    private val repository: ExpenseComposerRepository,
    private val calculateBalances: CalculateExpenseBalancesUseCase
) {
    operator fun invoke(groupId: String): Flow<ExpenseSummary?> =
        repository.observeGroup(groupId)
            .map { group -> group?.let(::summarize) }
            .distinctUntilChanged()

    private fun summarize(group: ExpenseGroup): ExpenseSummary {
        val current = ExpenseRevisions.current(group.expenses)
        val localBalance = group.localMemberId?.let { memberId ->
            calculateBalances(group.board, current, group.transfers).getOrNull()?.let { balances ->
                balances.firstOrNull { it.memberId == memberId }?.amountMinor ?: 0L
            }
        }
        return ExpenseSummary(
            boardId = group.board.id,
            currencyCode = group.board.currencyCode,
            expenseCount = current.size,
            totalMinor = current.sumOf(Expense::amountMinor),
            localBalanceMinor = localBalance
        )
    }
}
