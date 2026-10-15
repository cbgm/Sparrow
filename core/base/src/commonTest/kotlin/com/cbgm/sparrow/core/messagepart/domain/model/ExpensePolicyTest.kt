package com.cbgm.sparrow.core.messagepart.domain.model

import kotlin.test.Test
import kotlin.test.assertFailsWith

class ExpensePolicyTest {
    private val expense = Expense(
        id = "expense-1",
        boardId = "board-1",
        description = "Hotel in Rome",
        amountMinor = 10001L,
        currencyCode = "EUR",
        paidByMemberId = "alex",
        allocations = listOf(ExpenseAllocation("alex", 5000L), ExpenseAllocation("chris", 5001L)),
        occurredAtEpochMilliseconds = 1L
    )

    @Test fun validExpense() {
        ExpensePolicy.requireValid(expense)
        ExpensePolicy.requireValid(ExpenseBoard("board-1", "EUR", 1L))
    }

    @Test fun rejectsInvalidSplit() {
        assertFailsWith<IllegalArgumentException> {
            ExpensePolicy.requireValid(expense.copy(allocations = listOf(ExpenseAllocation("alex", 10000L))))
        }
    }

    @Test fun rejectsDuplicateParticipants() {
        assertFailsWith<IllegalArgumentException> {
            ExpensePolicy.requireValid(
                expense.copy(
                    allocations = listOf(
                        ExpenseAllocation("alex", 5000L),
                        ExpenseAllocation("alex", 5001L)
                    )
                )
            )
        }
    }

    @Test fun rejectsOverflowAllocations() {
        assertFailsWith<IllegalArgumentException> {
            ExpensePolicy.requireValid(
                expense.copy(
                    amountMinor = Long.MAX_VALUE,
                    allocations = listOf(ExpenseAllocation("alex", Long.MAX_VALUE), ExpenseAllocation("chris", 1L))
                )
            )
        }
    }
}
