package com.cbgm.sparrow.protocol.messagepart

import com.cbgm.sparrow.core.messagepart.data.model.ExpenseAllocationDto
import com.cbgm.sparrow.core.messagepart.data.model.ExpenseBoardDto
import com.cbgm.sparrow.core.messagepart.data.model.ExpenseDto
import com.cbgm.sparrow.protocol.message.GroupMessageContent
import kotlin.test.Test
import kotlin.test.assertFailsWith

class ExpenseWireValidationTest {
    private val board = ExpenseBoardDto("board", "EUR", 1L)
    private val expense = ExpenseDto(
        "expense",
        "board",
        "Hotel",
        500L,
        "EUR",
        "alex",
        listOf(ExpenseAllocationDto("alex", 250L), ExpenseAllocationDto("ben", 250L)),
        2L
    )

    @Test fun allowsStandaloneGroupExpenses() {
        GroupMessageContent(parts = listOf(board))
        GroupMessageContent(parts = listOf(expense))
    }

    @Test fun rejectsExpensesInDirectMessages() {
        assertFailsWith<IllegalArgumentException> { listOf(board).requireValidWireMessageParts() }
        assertFailsWith<IllegalArgumentException> { listOf(expense).requireValidWireMessageParts() }
    }

    @Test fun rejectsMixedExpenseMessages() {
        assertFailsWith<IllegalArgumentException> { GroupMessageContent(parts = listOf(board, expense)) }
    }
}
