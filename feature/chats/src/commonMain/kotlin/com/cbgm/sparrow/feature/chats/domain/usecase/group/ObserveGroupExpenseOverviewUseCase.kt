package com.cbgm.sparrow.feature.chats.domain.usecase.group

import com.cbgm.sparrow.core.messagepart.domain.model.Expense
import com.cbgm.sparrow.feature.attachments.domain.repository.MessageAttachmentOperationsRepository
import com.cbgm.sparrow.feature.chats.domain.model.group.GroupExpenseOverview
import com.cbgm.sparrow.feature.expenses.domain.ExpenseRevisions
import com.cbgm.sparrow.feature.expenses.domain.usecase.CalculateExpenseBalancesUseCase
import com.cbgm.sparrow.feature.expenses.domain.usecase.CalculateExpenseSettlementsUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

class ObserveGroupExpenseOverviewUseCase(
    private val getContext: GetGroupExpenseCreationContextUseCase,
    private val attachments: MessageAttachmentOperationsRepository,
    private val calculateBalances: CalculateExpenseBalancesUseCase,
    private val calculateSettlements: CalculateExpenseSettlementsUseCase
) {
    operator fun invoke(groupId: String): Flow<GroupExpenseOverview> =
        flow {
            val context = getContext(groupId).getOrThrow()
            emitAll(
                attachments.observeExpenses(groupId).map { all ->
                    val expenses = ExpenseRevisions.current(all.filter { it.boardId == context.board.id })
                        .sortedByDescending(Expense::occurredAtEpochMilliseconds)
                    val balances = calculateBalances(context.board, expenses).getOrThrow()
                    GroupExpenseOverview(
                        board = context.board,
                        localMemberId = context.localMemberId,
                        isLocalAdmin = context.isLocalAdmin,
                        members = context.members,
                        expenses = expenses,
                        balances = balances,
                        settlements = calculateSettlements(balances).getOrThrow()
                    )
                }
            )
        }
}
