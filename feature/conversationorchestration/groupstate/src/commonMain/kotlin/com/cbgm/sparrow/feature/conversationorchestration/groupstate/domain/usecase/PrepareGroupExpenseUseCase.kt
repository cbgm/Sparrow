package com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.usecase

import com.cbgm.sparrow.core.base.messagepart.domain.model.Expense
import com.cbgm.sparrow.core.base.messagepart.domain.model.ExpensePolicy
import com.cbgm.sparrow.core.util.id.IdGenerator
import com.cbgm.sparrow.core.util.result.safeSuspendCall
import com.cbgm.sparrow.core.util.time.SystemClock
import com.cbgm.sparrow.feature.conversationorchestration.api.command.GroupMessageCommand

/** Builds a new expense for the open board; payer and participants must still be group members. */
class PrepareGroupExpenseUseCase(
    private val findGroupExpenseMembers: FindGroupExpenseMembersUseCase
) {
    suspend operator fun invoke(command: GroupMessageCommand.CreateExpense): Result<Expense> =
        safeSuspendCall(logFailure = false) {
            val members = findGroupExpenseMembers(command.groupId).getOrThrow()
            members.requireCurrentBoard(command.boardId)
            members.requireMembers(command.paidByMemberId, command.allocations)
            val expense =
                Expense(
                    id = IdGenerator.generate(prefix = "expense"),
                    boardId = command.boardId,
                    description = command.description.trim(),
                    amountMinor = command.amountMinor,
                    currencyCode = members.board.currencyCode,
                    paidByMemberId = command.paidByMemberId,
                    allocations = command.allocations.sortedBy { it.memberId },
                    occurredAtEpochMilliseconds = SystemClock.nowEpochMilliseconds(),
                    category = command.category,
                    receipt = command.receipt,
                    createdByMemberId = members.localMemberId,
                    conversion = command.conversion
                )
            ExpensePolicy.requireValid(expense)
            expense
        }
}
