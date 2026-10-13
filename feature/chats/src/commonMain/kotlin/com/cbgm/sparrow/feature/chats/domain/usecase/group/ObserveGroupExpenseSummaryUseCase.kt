package com.cbgm.sparrow.feature.chats.domain.usecase.group

import com.cbgm.sparrow.core.messagepart.domain.model.Expense
import com.cbgm.sparrow.core.messagepart.domain.model.ExpenseBoard
import com.cbgm.sparrow.core.messagepart.domain.model.ExpenseMemberId
import com.cbgm.sparrow.feature.attachments.domain.repository.MessageAttachmentOperationsRepository
import com.cbgm.sparrow.feature.chats.domain.model.group.GroupExpenseSummary
import com.cbgm.sparrow.feature.chats.domain.repository.group.GroupPinReadRepository
import com.cbgm.sparrow.feature.expenses.domain.usecase.CalculateExpenseBalancesUseCase
import com.cbgm.sparrow.protocol.identity.LocalPublicIdentityProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

/** Observes the pinned active expense board together with its expenses and the local member's balance. */
@OptIn(ExperimentalCoroutinesApi::class)
class ObserveGroupExpenseSummaryUseCase(
    private val pins: GroupPinReadRepository,
    private val attachments: MessageAttachmentOperationsRepository,
    private val localIdentity: LocalPublicIdentityProvider,
    private val calculateBalances: CalculateExpenseBalancesUseCase
) {
    operator fun invoke(groupId: String): Flow<GroupExpenseSummary?> =
        pins.observe(groupId)
            .map { pin ->
                pin?.message?.parts
                    ?.filterIsInstance<ExpenseBoard>()
                    ?.singleOrNull { board -> board.closedAtEpochMilliseconds == null }
            }.distinctUntilChanged()
            .flatMapLatest { board ->
                if (board == null) return@flatMapLatest flowOf<GroupExpenseSummary?>(null)
                flow<GroupExpenseSummary?> {
                    val localMemberId = localIdentity.getLocalPublicIdentity().getOrNull()
                        ?.signingPublicKey
                        ?.let(ExpenseMemberId::fromSigningPublicKey)
                    emitAll(
                        attachments.observeExpenses(groupId).map { expenses ->
                            summarize(board, expenses.filter { it.boardId == board.id }, localMemberId)
                        }
                    )
                }
            }.distinctUntilChanged()

    private fun summarize(
        board: ExpenseBoard,
        expenses: List<Expense>,
        localMemberId: String?
    ): GroupExpenseSummary {
        // A later version of the same expense replaces the earlier one.
        val current = expenses.associateBy(Expense::id).values.toList()
        val localBalance = localMemberId?.let { memberId ->
            calculateBalances(board, current).getOrNull()?.let { balances ->
                balances.firstOrNull { it.memberId == memberId }?.amountMinor ?: 0L
            }
        }
        return GroupExpenseSummary(
            boardId = board.id,
            currencyCode = board.currencyCode,
            expenseCount = current.size,
            totalMinor = current.sumOf(Expense::amountMinor),
            localBalanceMinor = localBalance
        )
    }
}
