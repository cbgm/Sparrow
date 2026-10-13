package com.cbgm.sparrow.feature.chats.presentation.group.kmapper

import com.cbgm.sparrow.core.messagepart.domain.model.Expense
import com.cbgm.sparrow.core.messagepart.domain.model.ExpenseAllocation
import com.cbgm.sparrow.core.messagepart.domain.model.ExpenseCategory
import com.cbgm.sparrow.core.messagepart.domain.model.ExpenseSplit
import com.cbgm.sparrow.feature.chats.domain.model.group.GroupExpenseCreationContext
import com.cbgm.sparrow.feature.chats.domain.model.group.GroupExpenseMember
import com.cbgm.sparrow.feature.expenses.presentation.create.model.CreateExpenseUiState
import com.cbgm.sparrow.feature.expenses.presentation.create.model.ExpenseCategoryUi
import com.cbgm.sparrow.feature.expenses.presentation.create.model.ExpenseDraftUi
import com.cbgm.sparrow.feature.expenses.presentation.create.model.ExpenseParticipantUi
import com.cbgm.sparrow.feature.expenses.presentation.create.model.ExpenseSplitModeUi

internal fun GroupExpenseCreationContext.toCreateExpenseUiState(): CreateExpenseUiState {
    val participants = members.map { member -> member.toExpenseParticipantUi() }
    return CreateExpenseUiState(
        boardId = board.id,
        currencyCode = board.currencyCode,
        participants = participants,
        draft = expense?.toExpenseDraftUi() ?: ExpenseDraftUi(
            payerId = localMemberId,
            participantIds = participants.mapTo(mutableSetOf()) { it.id }
        ),
        isLoading = false,
        isEditing = expense != null
    )
}

private fun Expense.toExpenseDraftUi(): ExpenseDraftUi {
    val participantIds = allocations.mapTo(mutableSetOf(), ExpenseAllocation::memberId)
    val isEqualSplit = ExpenseSplit.evenly(amountMinor, participantIds) == allocations.sortedBy(ExpenseAllocation::memberId)
    return ExpenseDraftUi(
        description = description,
        amount = amountMinor.toAmountText('.'),
        payerId = paidByMemberId,
        participantIds = participantIds,
        category = ExpenseCategoryUi.valueOf(category.name),
        splitMode = if (isEqualSplit) ExpenseSplitModeUi.EQUAL else ExpenseSplitModeUi.CUSTOM,
        customAmounts = if (isEqualSplit) emptyMap() else allocations.associate { it.memberId to it.amountMinor.toAmountText('.') }
    )
}

internal fun ExpenseDraftUi.toEditedExpense(original: Expense): Expense? {
    val amount = totalMinor ?: return null
    val allocations = toExpenseAllocations() ?: return null
    return original.copy(
        description = description.trim(),
        amountMinor = amount,
        paidByMemberId = payerId,
        allocations = allocations,
        category = category.toExpenseCategory()
    )
}

internal fun GroupExpenseMember.toExpenseParticipantUi(): ExpenseParticipantUi =
    ExpenseParticipantUi(id, displayName, isLocal, avatarContactId)

internal fun ExpenseCategoryUi.toExpenseCategory(): ExpenseCategory = ExpenseCategory.valueOf(name)

internal fun ExpenseDraftUi.toExpenseAllocations(): List<ExpenseAllocation>? =
    allocationsMinor()?.map { (memberId, amountMinor) -> ExpenseAllocation(memberId, amountMinor) }

/** The next draft keeps payer, participants, category and split mode; custom amounts depend on the amount. */
internal fun ExpenseDraftUi.nextDraft(): ExpenseDraftUi =
    ExpenseDraftUi(
        payerId = payerId,
        participantIds = participantIds,
        category = category,
        splitMode = splitMode
    )

/** Switching to a custom split starts from the equal shares, so the user only adjusts the differences. */
internal fun ExpenseDraftUi.withSplitMode(mode: ExpenseSplitModeUi): ExpenseDraftUi {
    if (mode == splitMode) return this
    if (mode == ExpenseSplitModeUi.EQUAL) return copy(splitMode = mode, customAmounts = emptyMap())
    val equalShares = copy(splitMode = ExpenseSplitModeUi.EQUAL).allocationsMinor().orEmpty()
    return copy(
        splitMode = mode,
        customAmounts = equalShares.mapValues { (_, amountMinor) -> amountMinor.toAmountText(if (',' in amount) ',' else '.') }
    )
}

internal fun String.toAmountInput(): String = filter { it.isDigit() || it == '.' || it == ',' }.take(24)

private fun Long.toAmountText(separator: Char): String = "${this / 100}$separator${(this % 100).toString().padStart(2, '0')}"
