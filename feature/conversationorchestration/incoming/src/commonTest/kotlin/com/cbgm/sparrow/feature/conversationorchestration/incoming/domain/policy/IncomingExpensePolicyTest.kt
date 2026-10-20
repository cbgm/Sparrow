package com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.policy

import com.cbgm.sparrow.core.base.messagepart.domain.model.Expense
import com.cbgm.sparrow.core.base.messagepart.domain.model.ExpenseAllocation
import kotlin.test.Test
import kotlin.test.assertFailsWith

class IncomingExpensePolicyTest {
    private val taxi = Expense(
        id = "taxi",
        boardId = "board",
        description = "Taxi",
        amountMinor = 500L,
        currencyCode = "EUR",
        paidByMemberId = "bob",
        allocations = listOf(ExpenseAllocation("bob", 500L)),
        occurredAtEpochMilliseconds = 1L,
        createdByMemberId = "bob"
    )
    private val edited = taxi.copy(id = "taxi-2", revisionOf = "taxi", description = "Taxi home", editedAtEpochMilliseconds = 2L)
    private val deleted = taxi.copy(id = "taxi-3", revisionOf = "taxi", deletedAtEpochMilliseconds = 3L)

    @Test
    fun newExpenseMustBeCreatedBySenderOrReplayedByAnAdmin() {
        IncomingExpensePolicy.requireAuthorized(taxi, emptyList(), senderMemberId = "bob", senderIsAdmin = false)
        IncomingExpensePolicy.requireAuthorized(taxi, emptyList(), senderMemberId = "mia", senderIsAdmin = true)
        assertFailsWith<IllegalArgumentException> {
            IncomingExpensePolicy.requireAuthorized(taxi, emptyList(), senderMemberId = "mia", senderIsAdmin = false)
        }
        assertFailsWith<IllegalArgumentException> {
            IncomingExpensePolicy.requireAuthorized(edited, emptyList(), senderMemberId = "bob", senderIsAdmin = false)
        }
    }

    @Test
    fun creatorAndAdminsMayChangeExpense() {
        IncomingExpensePolicy.requireAuthorized(edited, listOf(taxi), senderMemberId = "bob", senderIsAdmin = false)
        IncomingExpensePolicy.requireAuthorized(deleted, listOf(taxi, edited), senderMemberId = "mia", senderIsAdmin = true)
        assertFailsWith<IllegalArgumentException> {
            IncomingExpensePolicy.requireAuthorized(deleted, listOf(taxi), senderMemberId = "mia", senderIsAdmin = false)
        }
    }

    @Test
    fun creatorCannotBeRewrittenAndIdsCannotBeReused() {
        assertFailsWith<IllegalArgumentException> {
            IncomingExpensePolicy.requireAuthorized(
                edited.copy(createdByMemberId = "mia"),
                listOf(taxi),
                senderMemberId = "mia",
                senderIsAdmin = true
            )
        }
        assertFailsWith<IllegalArgumentException> {
            IncomingExpensePolicy.requireAuthorized(taxi, listOf(taxi), senderMemberId = "bob", senderIsAdmin = false)
        }
    }
}
