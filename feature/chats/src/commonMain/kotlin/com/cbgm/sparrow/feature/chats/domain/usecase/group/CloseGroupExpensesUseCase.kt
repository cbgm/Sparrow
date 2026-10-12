package com.cbgm.sparrow.feature.chats.domain.usecase.group

import com.cbgm.sparrow.core.id.IdGenerator
import com.cbgm.sparrow.core.messagepart.domain.model.ExpenseBoard
import com.cbgm.sparrow.core.messagepart.domain.model.ExpenseBoardClosed
import com.cbgm.sparrow.core.result.safeSuspendCall
import com.cbgm.sparrow.core.time.SystemClock
import com.cbgm.sparrow.feature.chats.domain.repository.group.GroupPinRepository
import kotlinx.coroutines.flow.first

class CloseGroupExpensesUseCase(
    private val repository: GroupPinRepository,
    private val sendGroupMessage: SendGroupMessageUseCase
) {
    suspend operator fun invoke(groupId: String): Result<Unit> = safeSuspendCall {
        repository.requireCanPin(groupId).getOrThrow()
        val board = repository.observe(groupId).first()?.message?.parts
            ?.filterIsInstance<ExpenseBoard>()
            ?.firstOrNull { it.closedAtEpochMilliseconds == null }
            ?: error("There is no active expense board")

        // Closing is an ordinary encrypted group event, not a locally fabricated UI notice.
        // Queue it before releasing the pin so a failed send doesn't falsely close the board.
        sendGroupMessage(
            groupId,
            listOf(
                ExpenseBoardClosed(
                    id = IdGenerator.generate(prefix = "expense-closed"),
                    boardId = board.id,
                    closedAtEpochMilliseconds = SystemClock.nowEpochMilliseconds()
                )
            )
        ).getOrThrow()
        repository.closeExpenses(groupId).getOrThrow()
    }
}
