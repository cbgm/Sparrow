package com.cbgm.sparrow.feature.chats.presentation.group.kmapper

import com.cbgm.sparrow.core.messagepart.domain.model.ExpenseAllocation
import com.cbgm.sparrow.core.messagepart.domain.model.ExpenseCategory
import com.cbgm.sparrow.feature.chats.domain.model.group.GroupExpenseCreationContext
import com.cbgm.sparrow.feature.expenses.presentation.create.model.CreateExpenseUiState
import com.cbgm.sparrow.feature.expenses.presentation.create.model.ExpenseCategoryUi
import com.cbgm.sparrow.feature.expenses.presentation.create.model.ExpenseDraftUi
import com.cbgm.sparrow.feature.expenses.presentation.create.model.ExpenseParticipantUi
import com.cbgm.sparrow.feature.expenses.presentation.create.model.ExpenseSplitModeUi

internal fun GroupExpenseCreationContext.toCreateExpenseUiState(): CreateExpenseUiState {
    val participants = members.map { member ->
        ExpenseParticipantUi(member.id, member.displayName, member.isLocal, member.avatarContactId)
    }
    return CreateExpenseUiState(
        boardId = board.id,
        currencyCode = board.currencyCode,
        participants = participants,
        draft = ExpenseDraftUi(
            payerId = localMemberId,
            participantIds = participants.mapTo(mutableSetOf()) { it.id }
        ),
        isLoading = false
    )
}

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
