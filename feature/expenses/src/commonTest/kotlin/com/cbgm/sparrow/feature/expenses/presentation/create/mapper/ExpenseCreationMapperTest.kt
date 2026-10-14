package com.cbgm.sparrow.feature.expenses.presentation.create.mapper

import com.cbgm.sparrow.core.messagepart.domain.model.Expense
import com.cbgm.sparrow.core.messagepart.domain.model.ExpenseAllocation
import com.cbgm.sparrow.core.messagepart.domain.model.ExpenseBoard
import com.cbgm.sparrow.core.messagepart.domain.model.Image
import com.cbgm.sparrow.feature.expenses.domain.model.ExpenseBalance
import com.cbgm.sparrow.feature.expenses.domain.model.ExpenseCreationContext
import com.cbgm.sparrow.feature.expenses.domain.model.ExpenseMember
import com.cbgm.sparrow.feature.expenses.domain.usecase.CalculateExpenseBalancesUseCase
import com.cbgm.sparrow.feature.expenses.presentation.create.model.ExpenseDraftUi
import com.cbgm.sparrow.feature.expenses.presentation.create.model.ExpenseSplitModeUi
import com.cbgm.sparrow.feature.media.presentation.model.MediaSourceUi
import com.cbgm.sparrow.feature.media.presentation.model.MediaTypeUi
import com.cbgm.sparrow.feature.media.presentation.model.VisualMediaSelectionUi
import kotlin.test.Test
import kotlin.test.assertEquals

class ExpenseCreationMapperTest {
    @Test
    fun customSplitReducesDebtByOthersShare() {
        val board = ExpenseBoard("board", "EUR", 1L)
        // Chris paid 12 € split equally: Bob owes Chris 6 €.
        val dinner = Expense("dinner", "board", "Dinner", 1200L, "EUR", "chris", listOf(ExpenseAllocation("bob", 600L), ExpenseAllocation("chris", 600L)), 2L)
        // Bob pays 5 €: 0,50 € for himself, 4,50 € for Chris.
        val draft = ExpenseDraftUi("Taxi", "5", "bob", setOf("bob", "chris"))
            .withSplitMode(ExpenseSplitModeUi.CUSTOM)
            .copy(customAmounts = mapOf("bob" to "0,50", "chris" to "4,50"))
        val allocations = draft.toExpenseAllocations()!!
        val taxi = Expense("taxi", "board", "Taxi", 500L, "EUR", "bob", allocations, 3L)

        val balances = CalculateExpenseBalancesUseCase()(board, listOf(dinner, taxi)).getOrThrow()

        assertEquals(listOf(ExpenseBalance("bob", -150L), ExpenseBalance("chris", 150L)), balances)
    }

    @Test
    fun editingPrefillsDraftAndKeepsExpenseIdentity() {
        val board = ExpenseBoard("board", "EUR", 1L)
        val taxi = Expense(
            "taxi",
            "board",
            "Taxi",
            500L,
            "EUR",
            "bob",
            listOf(ExpenseAllocation("bob", 50L), ExpenseAllocation("chris", 450L)),
            3L
        )
        val context = ExpenseCreationContext(
            board = board,
            localMemberId = "chris",
            members = listOf(ExpenseMember("chris", "Chris", true), ExpenseMember("bob", "Bob", false)),
            expense = taxi
        )

        val state = context.toCreateExpenseUiState()

        assertEquals(true, state.isEditing)
        assertEquals(ExpenseSplitModeUi.CUSTOM, state.draft.splitMode)
        assertEquals(mapOf("bob" to "0.50", "chris" to "4.50"), state.draft.customAmounts)

        val edited = state.draft.copy(description = "Taxi home", customAmounts = mapOf("bob" to "1.00", "chris" to "4.00"))
            .toEditedExpense(taxi)!!

        assertEquals("taxi", edited.id)
        assertEquals(3L, edited.occurredAtEpochMilliseconds)
        assertEquals("Taxi home", edited.description)
        assertEquals(listOf(ExpenseAllocation("bob", 100L), ExpenseAllocation("chris", 400L)), edited.allocations)
    }

    @Test
    fun editingKeepsTheReceiptUnlessANewOneWasScanned() {
        val original = Image("old-receipt", "image/jpeg", 10L, 10, 20)
        val taxi = Expense(
            "taxi",
            "board",
            "Taxi",
            500L,
            "EUR",
            "bob",
            listOf(ExpenseAllocation("bob", 250L), ExpenseAllocation("chris", 250L)),
            3L,
            receipt = original
        )
        val draft = ExpenseDraftUi("Taxi", "5", "bob", setOf("bob", "chris"))
        val scanned =
            VisualMediaSelectionUi(
                id = "camera-image-1",
                localFilePath = "/media/receipt.jpg",
                byteSize = 42L,
                mimeType = "image/jpeg",
                source = MediaSourceUi.CAMERA,
                type = MediaTypeUi.IMAGE,
                width = 30,
                height = 60
            )

        val kept = draft.toEditedExpense(taxi)!!
        val replaced = draft.copy(receipt = scanned).toEditedExpense(taxi)!!

        assertEquals(original, kept.receipt)
        assertEquals(
            Image("camera-image-1", "image/jpeg", 42L, 30, 60, localFilePath = "/media/receipt.jpg"),
            replaced.receipt
        )
    }
}
