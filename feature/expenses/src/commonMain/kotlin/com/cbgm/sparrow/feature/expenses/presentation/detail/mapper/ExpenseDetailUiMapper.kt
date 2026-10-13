package com.cbgm.sparrow.feature.expenses.presentation.detail.mapper

import com.cbgm.sparrow.core.messagepart.domain.model.Expense
import com.cbgm.sparrow.core.messagepart.domain.model.ExpenseAllocation
import com.cbgm.sparrow.core.messagepart.domain.model.ExpenseSplit
import com.cbgm.sparrow.core.time.formatDate
import com.cbgm.sparrow.feature.expenses.domain.model.ExpenseOverview
import com.cbgm.sparrow.feature.expenses.presentation.common.mapper.toExpenseAmountLabel
import com.cbgm.sparrow.feature.expenses.presentation.create.mapper.toExpenseParticipantUi
import com.cbgm.sparrow.feature.expenses.presentation.create.model.ExpenseCategoryUi
import com.cbgm.sparrow.feature.expenses.presentation.create.model.ExpenseParticipantUi
import com.cbgm.sparrow.feature.expenses.presentation.detail.model.ExpenseDetailUiState
import com.cbgm.sparrow.feature.expenses.presentation.detail.model.ExpenseShareUi

internal fun ExpenseOverview.toExpenseDetailUiState(expense: Expense): ExpenseDetailUiState {
    val participants = members.associate { member -> member.id to member.toExpenseParticipantUi() }

    fun participant(id: String) =
        participants[id] ?: ExpenseParticipantUi(id, "", isLocal = id == localMemberId)

    val sortedAllocations = expense.allocations.sortedBy(ExpenseAllocation::memberId)
    val memberIds = sortedAllocations.mapTo(mutableSetOf()) { it.memberId }
    val isEqualSplit = ExpenseSplit.evenly(expense.amountMinor, memberIds) == sortedAllocations
    return ExpenseDetailUiState(
        isLoading = false,
        description = expense.description,
        amountLabel = expense.amountMinor.toExpenseAmountLabel(expense.currencyCode),
        category = ExpenseCategoryUi.valueOf(expense.category.name),
        payer = participant(expense.paidByMemberId),
        dateLabel = formatDate(expense.occurredAtEpochMilliseconds),
        shares = expense.allocations
            .sortedWith(
                compareByDescending<ExpenseAllocation> { it.memberId == localMemberId }
                    .thenByDescending { it.amountMinor }
            )
            .map { allocation ->
                ExpenseShareUi(
                    member = participant(allocation.memberId),
                    amountLabel = allocation.amountMinor.toExpenseAmountLabel(expense.currencyCode)
                )
            },
        equalShareLabel = if (isEqualSplit) {
            sortedAllocations
                .maxOf(ExpenseAllocation::amountMinor)
                .toExpenseAmountLabel(expense.currencyCode)
        } else {
            null
        },
        canChange = isLocalAdmin || expense.createdByMemberId == localMemberId
    )
}
