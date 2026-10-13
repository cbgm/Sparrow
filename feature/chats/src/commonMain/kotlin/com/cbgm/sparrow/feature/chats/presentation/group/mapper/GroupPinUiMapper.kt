package com.cbgm.sparrow.feature.chats.presentation.group.mapper

import com.cbgm.sparrow.feature.chats.presentation.common.history.mapper.activeExpenseBoard
import com.cbgm.sparrow.feature.chats.presentation.group.model.ExpenseSummaryLoadUi
import com.cbgm.sparrow.feature.chats.presentation.group.model.GroupConversationUiState
import com.cbgm.sparrow.feature.chats.presentation.group.model.GroupPinUi

internal fun GroupConversationUiState.toGroupPinUi(expenseSummary: ExpenseSummaryLoadUi): GroupPinUi? {
    val message = pinnedMessage ?: return null
    val asMessage = GroupPinUi.Message(
        message = message,
        pinnedAtEpochMilliseconds = pinnedAtEpochMilliseconds,
        canUnpin = isLocalAdmin && !expensesActive
    )
    if (message.activeExpenseBoard == null) return asMessage
    return when (expenseSummary) {
        is ExpenseSummaryLoadUi.Loaded -> expenseSummary.summary?.let(GroupPinUi::Expenses)
        ExpenseSummaryLoadUi.Loading -> null
        ExpenseSummaryLoadUi.Failed -> asMessage
    }
}
