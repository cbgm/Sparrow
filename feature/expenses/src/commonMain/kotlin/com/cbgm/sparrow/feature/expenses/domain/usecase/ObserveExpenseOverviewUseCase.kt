package com.cbgm.sparrow.feature.expenses.domain.usecase

import com.cbgm.sparrow.core.base.messagepart.domain.model.Expense
import com.cbgm.sparrow.feature.expenses.domain.ExpensePayments
import com.cbgm.sparrow.feature.expenses.domain.ExpenseRevisions
import com.cbgm.sparrow.feature.expenses.domain.model.ExpenseOverview
import com.cbgm.sparrow.feature.expenses.domain.repository.ExpenseComposerRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ObserveExpenseOverviewUseCase(
    private val repository: ExpenseComposerRepository,
    private val calculateBalances: CalculateExpenseBalancesUseCase,
    private val calculateSettlements: CalculateExpenseSettlementsUseCase
) {
    operator fun invoke(groupId: String): Flow<ExpenseOverview> =
        repository.observeGroup(groupId).map { group ->
            requireNotNull(group) { "Group expenses are not active" }
            val expenses = ExpenseRevisions.current(group.expenses)
                .sortedByDescending(Expense::occurredAtEpochMilliseconds)
            val balances = calculateBalances(group.board, expenses, group.transfers).getOrThrow()
            val paidByMember = expenses.groupBy(Expense::paidByMemberId)
                .mapValues { (_, paid) -> paid.fold(0L) { sum, expense -> addAmountsSafely(sum, expense.amountMinor) } }
                .toMutableMap()
            ExpensePayments.confirmed(group.board, group.transfers).forEach { payment ->
                paidByMember[payment.fromMemberId] = addAmountsSafely(paidByMember[payment.fromMemberId] ?: 0L, payment.amountMinor)
                paidByMember[payment.toMemberId] = addAmountsSafely(paidByMember[payment.toMemberId] ?: 0L, -payment.amountMinor)
            }
            ExpenseOverview(
                board = group.board,
                localMemberId = group.localMemberId ?: error("Local expense member is missing"),
                isLocalAdmin = group.isLocalAdmin,
                members = group.members,
                expenses = expenses,
                balances = balances,
                settlements = calculateSettlements(balances).getOrThrow(),
                transfers = group.transfers,
                paidByMember = paidByMember
            )
        }
}
