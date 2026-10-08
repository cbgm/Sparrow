package com.cbgm.sparrow.core.messagepart.data.mapper

import com.cbgm.sparrow.core.messagepart.data.model.ExpenseAllocationDto
import com.cbgm.sparrow.core.messagepart.data.model.ExpenseBoardDto
import com.cbgm.sparrow.core.messagepart.data.model.ExpenseDto
import com.cbgm.sparrow.core.messagepart.data.model.ImageDto
import com.cbgm.sparrow.core.messagepart.domain.model.Expense
import com.cbgm.sparrow.core.messagepart.domain.model.ExpenseBoard
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class ExpensePartDtoMapperTest {
    @Test fun boardRoundTrip() {
        val board = assertIs<ExpenseBoard>(ExpenseBoardDto("board", "EUR", 42L).toMessagePart())
        assertEquals("EUR", board.currencyCode)
        assertEquals(42L, assertIs<ExpenseBoardDto>(board.toDto()).activatedAtEpochMilliseconds)
    }

    @Test fun expenseRoundTripPreservesReceipt() {
        val dto = ExpenseDto(
            id = "expense",
            boardId = "board",
            description = "Museum tickets",
            amountMinor = 1001L,
            currencyCode = "EUR",
            paidByMemberId = "alex",
            allocations = listOf(ExpenseAllocationDto("alex", 500L), ExpenseAllocationDto("mia", 501L)),
            occurredAtEpochMilliseconds = 43L,
            receipt = ImageDto("receipt", mimeType = "image/jpeg", byteSize = 100L, width = 20, height = 10)
        )
        val expense = assertIs<Expense>(dto.toMessagePart())
        assertEquals("Museum tickets", expense.description)
        assertEquals("receipt", expense.receipt?.id)
        val roundTrip = assertIs<ExpenseDto>(expense.toDto())
        assertEquals(dto.allocations, roundTrip.allocations)
        assertEquals(dto.receipt, roundTrip.receipt)
    }
}
