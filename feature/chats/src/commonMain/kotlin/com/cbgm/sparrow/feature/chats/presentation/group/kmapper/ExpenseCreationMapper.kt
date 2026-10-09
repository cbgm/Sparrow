package com.cbgm.sparrow.feature.chats.presentation.group.kmapper

import com.cbgm.sparrow.core.messagepart.domain.model.ExpenseCategory
import com.cbgm.sparrow.feature.chats.domain.model.group.GroupExpenseCreationContext
import com.cbgm.sparrow.feature.expenses.presentation.create.model.CreateExpenseUiState
import com.cbgm.sparrow.feature.expenses.presentation.create.model.ExpenseCategoryUi
import com.cbgm.sparrow.feature.expenses.presentation.create.model.ExpenseDraftUi
import com.cbgm.sparrow.feature.expenses.presentation.create.model.ExpenseParticipantUi

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
