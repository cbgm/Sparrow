package com.cbgm.sparrow.core.base.messagepart.data.mapper

import com.cbgm.sparrow.core.base.messagepart.data.model.ExpenseAllocationDto
import com.cbgm.sparrow.core.base.messagepart.data.model.ExpenseBoardDto
import com.cbgm.sparrow.core.base.messagepart.data.model.ExpenseConversionDto
import com.cbgm.sparrow.core.base.messagepart.data.model.ExpenseDto
import com.cbgm.sparrow.core.base.messagepart.data.model.ImageDto
import com.cbgm.sparrow.core.base.messagepart.domain.model.Expense
import com.cbgm.sparrow.core.base.messagepart.domain.model.ExpenseBoard
import com.cbgm.sparrow.core.base.messagepart.domain.model.ExpenseCategory
import com.cbgm.sparrow.core.base.messagepart.domain.model.ExpenseConversion
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class ExpensePartDtoMapperTest {
    @Test fun boardRoundTrip() {
        val board = assertIs<ExpenseBoard>(ExpenseBoardDto("board", "EUR", 42L).toMessagePart())
        assertEquals("EUR", board.currencyCode)
        assertEquals(42L, assertIs<ExpenseBoardDto>(board.toDto()).activatedAtEpochMilliseconds)
    }

    @Test fun expenseRoundTripPreservesReceiptAndCategory() {
        val dto = ExpenseDto(
            id = "expense",
            boardId = "board",
            description = "Museum tickets",
            amountMinor = 1001L,
            currencyCode = "EUR",
            paidByMemberId = "alex",
            allocations = listOf(ExpenseAllocationDto("alex", 500L), ExpenseAllocationDto("mia", 501L)),
            occurredAtEpochMilliseconds = 43L,
            receipt = ImageDto("receipt", mimeType = "image/jpeg", byteSize = 100L, width = 20, height = 10),
            category = "ACTIVITY"
        )
        val expense = assertIs<Expense>(dto.toMessagePart())
        assertEquals("Museum tickets", expense.description)
        assertEquals("receipt", expense.receipt?.id)
        assertEquals(ExpenseCategory.ACTIVITY, expense.category)
        val roundTrip = assertIs<ExpenseDto>(expense.toDto())
        assertEquals(dto.allocations, roundTrip.allocations)
        assertEquals(dto.receipt, roundTrip.receipt)
        assertEquals("ACTIVITY", roundTrip.category)
    }

    @Test fun oldExpenseWithoutCategoryUsesOther() {
        val dto = ExpenseDto(
            id = "legacy",
            boardId = "board",
            description = "Old expense",
            amountMinor = 100L,
            currencyCode = "EUR",
            paidByMemberId = "alex",
            allocations = listOf(ExpenseAllocationDto("alex", 100L)),
            occurredAtEpochMilliseconds = 1L
        )
        assertEquals(ExpenseCategory.OTHER, assertIs<Expense>(dto.toMessagePart()).category)
    }

    @Test fun convertedExpenseKeepsOriginalAmountAndRate() {
        val dto = ExpenseDto(
            id = "dinner",
            boardId = "board",
            description = "Dinner",
            amountMinor = 4_465L,
            currencyCode = "EUR",
            paidByMemberId = "alex",
            allocations = listOf(ExpenseAllocationDto("alex", 4_465L)),
            occurredAtEpochMilliseconds = 1L,
            conversion = ExpenseConversionDto(5_000L, "USD", "0.89294")
        )

        val expense = assertIs<Expense>(dto.toMessagePart())

        assertEquals(ExpenseConversion(5_000L, "USD", "0.89294"), expense.conversion)
        assertEquals(dto.conversion, assertIs<ExpenseDto>(expense.toDto()).conversion)
    }
}
