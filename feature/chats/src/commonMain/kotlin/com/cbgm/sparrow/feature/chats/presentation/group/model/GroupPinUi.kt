package com.cbgm.sparrow.feature.chats.presentation.group.model

import com.cbgm.sparrow.feature.chats.presentation.common.history.model.MessageBubbleUi
import com.cbgm.sparrow.feature.expenses.presentation.pin.model.ExpensePinUi

sealed interface GroupPinUi {
    data class Message(
        val message: MessageBubbleUi,
        val pinnedAtEpochMilliseconds: Long,
        val canUnpin: Boolean
    ) : GroupPinUi

    data class Expenses(
        val summary: ExpensePinUi
    ) : GroupPinUi
}

sealed interface ExpenseSummaryLoadUi {
    data object Loading : ExpenseSummaryLoadUi

    data class Loaded(
        val summary: ExpensePinUi?
    ) : ExpenseSummaryLoadUi

    data object Failed : ExpenseSummaryLoadUi
}
