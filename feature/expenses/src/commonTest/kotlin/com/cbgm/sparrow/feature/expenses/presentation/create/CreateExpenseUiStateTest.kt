package com.cbgm.sparrow.feature.expenses.presentation.create

import com.cbgm.sparrow.feature.expenses.presentation.create.model.CreateExpenseUiState
import com.cbgm.sparrow.feature.expenses.presentation.create.model.ExpenseDraftUi
import com.cbgm.sparrow.feature.expenses.presentation.create.model.ExpenseParticipantUi
import kotlin.test.Test
import kotlin.test.assertFalse
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
}
