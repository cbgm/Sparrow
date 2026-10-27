package com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.usecase

import com.cbgm.sparrow.core.base.messagepart.domain.model.ExpenseBoard
import com.cbgm.sparrow.core.base.messagepart.domain.model.ExpenseBoardClosed
import com.cbgm.sparrow.core.base.messagepart.domain.model.ExpensePolicy
import com.cbgm.sparrow.core.util.id.IdGenerator
import com.cbgm.sparrow.core.util.result.safeSuspendCall
import com.cbgm.sparrow.core.util.time.SystemClock

/** Builds the message parts that open and close a group expense board. */
class CreateExpenseBoardUseCase {
    suspend operator fun invoke(currencyCode: String): Result<ExpenseBoard> =
        safeSuspendCall(logFailure = false) {
            val board =
                ExpenseBoard(
                    id = IdGenerator.generate(prefix = "expense-board"),
                    currencyCode = currencyCode,
                    activatedAtEpochMilliseconds = SystemClock.nowEpochMilliseconds()
                )
            ExpensePolicy.requireValid(board)
            board
        }

    /** The closing announcement of [board]; members close their board when they receive it. */
    suspend fun closing(board: ExpenseBoard): Result<ExpenseBoardClosed> =
        safeSuspendCall(logFailure = false) {
            ExpenseBoardClosed(
                id = IdGenerator.generate(prefix = "expense-closed"),
                boardId = board.id,
                closedAtEpochMilliseconds = SystemClock.nowEpochMilliseconds()
            )
        }
}
