package com.cbgm.sparrow.feature.chats.domain.usecase.group

import com.cbgm.sparrow.core.id.IdGenerator
import com.cbgm.sparrow.core.messagepart.domain.model.ExpenseBoard
import com.cbgm.sparrow.core.messagepart.domain.model.ExpensePolicy
import com.cbgm.sparrow.core.result.safeSuspendCall
import com.cbgm.sparrow.core.time.SystemClock
import com.cbgm.sparrow.feature.chats.domain.repository.group.GroupPinRepository
import kotlinx.coroutines.flow.first

/** An expense board is an ordinary group attachment, pinned using the existing group pin flow. */
class ActivateGroupExpensesUseCase(
    private val sendGroupMessage: SendGroupMessageUseCase,
    private val pinGroupMessage: PinGroupMessageUseCase,
    private val pinRepository: GroupPinRepository
) {
    suspend operator fun invoke(groupId: String, currencyCode: String): Result<Unit> =
        safeSuspendCall {
            require(groupId.isNotBlank()) { "Group ID is missing" }
            pinRepository.requireCanPin(groupId).getOrThrow()
            val isActive = pinRepository.observe(groupId).first()?.message?.parts
                ?.filterIsInstance<ExpenseBoard>()
                ?.any { it.closedAtEpochMilliseconds == null } ?: false
            check(!isActive) { "Group expenses are already active" }

            val board = ExpenseBoard(
                id = IdGenerator.generate(prefix = "expense-board"),
                currencyCode = currencyCode,
                activatedAtEpochMilliseconds = SystemClock.nowEpochMilliseconds()
            )
            ExpensePolicy.requireValid(board)
            val messageId = sendGroupMessage(groupId, listOf(board)).getOrThrow()
            pinGroupMessage(groupId, messageId).getOrThrow()
        }
}
