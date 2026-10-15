package com.cbgm.sparrow.feature.chats.presentation.group.mapper

import com.cbgm.sparrow.core.message.domain.model.MessageContentStatus
import com.cbgm.sparrow.core.message.domain.model.MessageDeliveryStatus
import com.cbgm.sparrow.core.messagepart.ui.model.ExpenseBoardUi
import com.cbgm.sparrow.feature.chats.domain.model.MessageSecurity
import com.cbgm.sparrow.feature.chats.presentation.common.history.model.MessageBubbleUi
import com.cbgm.sparrow.feature.chats.presentation.group.model.ExpenseSummaryLoadUi
import com.cbgm.sparrow.feature.chats.presentation.group.model.GroupConversationUiState
import com.cbgm.sparrow.feature.chats.presentation.group.model.GroupPinUi
import com.cbgm.sparrow.feature.expenses.presentation.pin.model.ExpensePinBalanceUi
import com.cbgm.sparrow.feature.expenses.presentation.pin.model.ExpensePinUi
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class GroupPinUiMapperTest {
    private val message = MessageBubbleUi(
        id = "pinned",
        isMine = true,
        security = MessageSecurity.END_TO_END_ENCRYPTED,
        contentStatus = MessageContentStatus.READABLE,
        deliveryStatus = MessageDeliveryStatus.DELIVERED
    )
    private val boardMessage = message.copy(
        parts = listOf(ExpenseBoardUi("board", "EUR", 1L))
    )
    private val summary = ExpensePinUi(2, "€10.00", ExpensePinBalanceUi.Settled)

    @Test
    fun regularPinShowsTheMessage() {
        val state = GroupConversationUiState(pinnedMessage = message, pinnedAtEpochMilliseconds = 5L, isLocalAdmin = true)

        assertEquals(GroupPinUi.Message(message, 5L, canUnpin = true), state.toGroupPinUi(ExpenseSummaryLoadUi.Loading))
    }

    @Test
    fun activeBoardWaitsForItsSummary() {
        val state = GroupConversationUiState(pinnedMessage = boardMessage, expensesActive = true)

        assertNull(state.toGroupPinUi(ExpenseSummaryLoadUi.Loading))
        assertNull(state.toGroupPinUi(ExpenseSummaryLoadUi.Loaded(null)))
        assertEquals(GroupPinUi.Expenses(summary), state.toGroupPinUi(ExpenseSummaryLoadUi.Loaded(summary)))
        assertEquals(
            GroupPinUi.Message(boardMessage, 0L, canUnpin = false),
            state.toGroupPinUi(ExpenseSummaryLoadUi.Failed)
        )
    }

    @Test
    fun noPinWithoutPinnedMessage() {
        assertNull(GroupConversationUiState().toGroupPinUi(ExpenseSummaryLoadUi.Loaded(summary)))
    }
}
