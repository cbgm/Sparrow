package com.cbgm.sparrow.feature.chats.presentation.group.mapper

import com.cbgm.sparrow.core.messagepart.domain.model.Expense
import com.cbgm.sparrow.core.messagepart.domain.model.expenseId
import com.cbgm.sparrow.core.time.formatDate
import com.cbgm.sparrow.feature.chats.domain.model.group.GroupExpenseOverview
import com.cbgm.sparrow.feature.chats.presentation.group.kmapper.toExpenseParticipantUi
import com.cbgm.sparrow.feature.expenses.presentation.common.mapper.toExpenseAmountLabel
import com.cbgm.sparrow.feature.expenses.presentation.create.model.ExpenseCategoryUi
import com.cbgm.sparrow.feature.expenses.presentation.create.model.ExpenseParticipantUi
import com.cbgm.sparrow.feature.expenses.presentation.overview.model.ExpenseAmountUi
import com.cbgm.sparrow.feature.expenses.presentation.overview.model.ExpenseListItemUi
import com.cbgm.sparrow.feature.expenses.presentation.overview.model.ExpenseMemberBalanceUi
import com.cbgm.sparrow.feature.expenses.presentation.overview.model.ExpenseOverviewUiState
import com.cbgm.sparrow.feature.expenses.presentation.overview.model.ExpenseSettlementUi
import com.cbgm.sparrow.feature.expenses.presentation.overview.model.ExpenseYourBalanceUi
import com.cbgm.sparrow.feature.expenses.presentation.overview.model.ExpenseYourPartUi
import kotlin.math.absoluteValue

internal fun GroupExpenseOverview.toExpenseOverviewUiState(): ExpenseOverviewUiState {
    val currency = board.currencyCode
    val participants = members.associate { member -> member.id to member.toExpenseParticipantUi() }

    fun participant(id: String) = participants[id] ?: ExpenseParticipantUi(id, "", isLocal = id == localMemberId)

    val balanceById = balances.associate { it.memberId to it.amountMinor }
    val memberIds = (members.map { it.id } + balanceById.keys).distinct()
    val largestBalance = balanceById.values.maxOfOrNull { it.absoluteValue }?.takeIf { it > 0L } ?: 1L

    return ExpenseOverviewUiState(
        isLoading = false,
        currencyCode = currency,
        activatedAtLabel = formatDate(board.activatedAtEpochMilliseconds),
        yourBalance = ExpenseYourBalanceUi(
            balance = (balanceById[localMemberId] ?: 0L).toExpenseAmountUi(currency),
            totalLabel = expenses.sumOf(Expense::amountMinor).toExpenseAmountLabel(currency),
            yourShareLabel = expenses.sumOf { it.shareOf(localMemberId) }.toExpenseAmountLabel(currency),
            youPaidLabel = expenses.filter { it.paidByMemberId == localMemberId }
                .sumOf(Expense::amountMinor)
                .toExpenseAmountLabel(currency),
            counterpartCount = settlements.count { it.fromMemberId == localMemberId || it.toMemberId == localMemberId }
        ),
        settlements = settlements.map { settlement ->
            ExpenseSettlementUi(
                from = participant(settlement.fromMemberId),
                to = participant(settlement.toMemberId),
                amountLabel = settlement.amountMinor.toExpenseAmountLabel(currency)
            )
        },
        balances = memberIds
            .map { id -> id to (balanceById[id] ?: 0L) }
            .sortedByDescending { (_, amount) -> amount }
            .map { (id, amount) ->
                ExpenseMemberBalanceUi(
                    member = participant(id),
                    balance = amount.toExpenseAmountUi(currency),
                    fraction = amount.absoluteValue.toFloat() / largestBalance
                )
            },
        expenses = expenses.map { expense ->
            ExpenseListItemUi(
                id = expense.expenseId,
                description = expense.description,
                category = ExpenseCategoryUi.valueOf(expense.category.name),
                payer = participant(expense.paidByMemberId),
                dateLabel = formatDate(expense.occurredAtEpochMilliseconds),
                amountLabel = expense.amountMinor.toExpenseAmountLabel(currency),
                yourPart = expense.yourPart(localMemberId),
                canChange = isLocalAdmin || expense.createdByMemberId == localMemberId
            )
        }
    )
}

private fun Expense.shareOf(memberId: String): Long = allocations.firstOrNull { it.memberId == memberId }?.amountMinor ?: 0L

private fun Expense.yourPart(localMemberId: String): ExpenseYourPartUi {
    val share = shareOf(localMemberId)
    return when {
        paidByMemberId == localMemberId && amountMinor > share ->
            ExpenseYourPartUi.Lent((amountMinor - share).toExpenseAmountLabel(currencyCode))
        paidByMemberId != localMemberId && share > 0L ->
            ExpenseYourPartUi.Borrowed(share.toExpenseAmountLabel(currencyCode))
        else -> ExpenseYourPartUi.NotInvolved
    }
}

private fun Long.toExpenseAmountUi(currency: String): ExpenseAmountUi =
    when {
        this < 0L -> ExpenseAmountUi.Owes((-this).toExpenseAmountLabel(currency))
        this > 0L -> ExpenseAmountUi.Owed(toExpenseAmountLabel(currency))
        else -> ExpenseAmountUi.Settled(0L.toExpenseAmountLabel(currency))
    }
