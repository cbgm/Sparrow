package com.cbgm.sparrow.feature.chats.domain.usecase.group

import com.cbgm.sparrow.core.id.IdGenerator
import com.cbgm.sparrow.core.messagepart.domain.model.Expense
import com.cbgm.sparrow.core.messagepart.domain.model.ExpenseAllocation
import com.cbgm.sparrow.core.messagepart.domain.model.ExpensePolicy
import com.cbgm.sparrow.core.result.safeSuspendCall
import com.cbgm.sparrow.core.time.SystemClock
import com.cbgm.sparrow.feature.expenses.domain.ExpenseMoney

/** Sends an Expense as an ordinary group MessagePart through the existing message pipeline. */
class CreateGroupExpenseUseCase(
    private val context: GetGroupExpenseCreationContextUseCase,
    private val sendGroupMessage: SendGroupMessageUseCase
) {
    suspend operator fun invoke(
        groupId: String,
        boardId: String,
        description: String,
        amountMinor: Long,
        paidByMemberId: String,
        participantIds: Set<String>
    ): Result<String> = safeSuspendCall {
        val current = context(groupId).getOrThrow()
        check(current.board.id == boardId) { "Expense board has changed" }
        val activeIds = current.members.mapTo(mutableSetOf()) { it.id }
        check(paidByMemberId in activeIds) { "Payer is no longer a group member" }
        check(participantIds.all { it in activeIds }) { "Expense includes a former group member" }
        check(current.board.currencyCode == "EUR") { "Unsupported expense currency" }
        val allocations = ExpenseMoney.splitEvenly(amountMinor, participantIds)
            .map { (memberId, share) -> ExpenseAllocation(memberId, share) }
        val expense = Expense(
            id = IdGenerator.generate("expense"),
            boardId = boardId,
            description = description.trim(),
            amountMinor = amountMinor,
            currencyCode = current.board.currencyCode,
            paidByMemberId = paidByMemberId,
            allocations = allocations,
            occurredAtEpochMilliseconds = SystemClock.nowEpochMilliseconds()
        )
        ExpensePolicy.requireValid(expense)
        sendGroupMessage(groupId, listOf(expense)).getOrThrow()
    }
}
