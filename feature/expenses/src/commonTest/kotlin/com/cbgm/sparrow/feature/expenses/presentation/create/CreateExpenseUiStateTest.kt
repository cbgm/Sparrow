package com.cbgm.sparrow.feature.expenses.presentation.create

import com.cbgm.sparrow.feature.expenses.presentation.create.model.CreateExpenseUiState
import com.cbgm.sparrow.feature.expenses.presentation.create.model.ExpenseDraftUi
import com.cbgm.sparrow.feature.expenses.presentation.create.model.ExpenseParticipantUi
import com.cbgm.sparrow.feature.expenses.presentation.create.model.ExpenseSplitModeUi
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CreateExpenseUiStateTest {
    private val members = listOf(
        ExpenseParticipantUi("memberA", "Alice"),
        ExpenseParticipantUi("memberB", "Bob")
    )
    private val draft = ExpenseDraftUi("Hotel", "12.01", "memberA", setOf("memberA", "memberB"))

    @Test fun validDraftCanBeAddedOrSent() {
        val state = CreateExpenseUiState(participants = members, draft = draft, isLoading = false)
        assertTrue(state.canAdd)
        assertTrue(state.canSend)
    }

    @Test fun queuedExpensesCanBeSentWithoutCurrentDraft() {
        val state = CreateExpenseUiState(
            participants = members,
            draft = ExpenseDraftUi(payerId = "memberA", participantIds = setOf("memberA", "memberB")),
            queued = listOf(draft),
            isLoading = false
        )
        assertFalse(state.canAdd)
        assertTrue(state.canSend)
    }

    @Test fun incompleteCurrentDraftCannotBeSilentlySent() {
        val state = CreateExpenseUiState(
            participants = members,
            draft = draft.copy(amount = ""),
            queued = listOf(draft),
            isLoading = false
        )
        assertFalse(state.canSend)
    }

    @Test fun invalidParticipantOrTinyAmountCannotBeAdded() {
        val state = CreateExpenseUiState(participants = members, draft = draft.copy(amount = "0.01"), isLoading = false)
        assertFalse(state.canAdd)
        assertFalse(state.canSend)
        assertFalse(state.copy(draft = draft.copy(participantIds = setOf("unknown"))).canAdd)
    }

    @Test fun customSplitMustMatchTotalExactly() {
        val custom = draft.copy(
            amount = "12,50",
            splitMode = ExpenseSplitModeUi.CUSTOM,
            customAmounts = mapOf("memberA" to "10", "memberB" to "2,50")
        )
        assertEquals(mapOf("memberA" to 1000L, "memberB" to 250L), custom.allocationsMinor())
        assertTrue(CreateExpenseUiState(participants = members, draft = custom, isLoading = false).canAdd)

        val exceeded = custom.copy(customAmounts = mapOf("memberA" to "10", "memberB" to "3"))
        assertEquals(1300L, exceeded.customAssignedMinor)
        assertNull(exceeded.allocationsMinor())
        assertNull(custom.copy(customAmounts = mapOf("memberA" to "12,50")).allocationsMinor())
        assertFalse(CreateExpenseUiState(participants = members, draft = exceeded, isLoading = false).canAdd)
    }

    @Test fun customSplitIgnoresUnselectedMembers() {
        val custom = draft.copy(
            amount = "5",
            participantIds = setOf("memberA"),
            splitMode = ExpenseSplitModeUi.CUSTOM,
            customAmounts = mapOf("memberA" to "5", "memberB" to "3")
        )
        assertEquals(mapOf("memberA" to 500L), custom.allocationsMinor())
    }
}
