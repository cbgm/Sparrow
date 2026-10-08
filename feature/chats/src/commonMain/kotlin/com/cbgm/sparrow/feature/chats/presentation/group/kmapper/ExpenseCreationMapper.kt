package com.cbgm.sparrow.feature.chats.presentation.group.kmapper

import com.cbgm.sparrow.feature.chats.domain.model.group.GroupExpenseCreationContext
import com.cbgm.sparrow.feature.expenses.presentation.create.model.CreateExpenseUiState
import com.cbgm.sparrow.feature.expenses.presentation.create.model.ExpenseParticipantUi

internal fun GroupExpenseCreationContext.toCreateExpenseUiState(): CreateExpenseUiState {
    val participants = members.map { member ->
        ExpenseParticipantUi(member.id, member.displayName, member.isLocal)
    }
    return CreateExpenseUiState(
        boardId = board.id,
        currencyCode = board.currencyCode,
        payerId = localMemberId,
        participants = participants,
        selectedParticipantIds = participants.mapTo(mutableSetOf()) { it.id },
        isLoading = false
    )
}
