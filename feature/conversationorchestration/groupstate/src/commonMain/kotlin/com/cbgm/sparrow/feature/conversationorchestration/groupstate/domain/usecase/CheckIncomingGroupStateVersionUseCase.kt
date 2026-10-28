package com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.usecase

import com.cbgm.sparrow.core.util.result.safeSuspendCall
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.model.GroupPinSlotPolicy
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.model.GroupStateVersionPolicy
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.repository.GroupPinRepository

/**
 * Whether a received pin replaces the stored one ([GroupStateVersionPolicy]), checked before its sender is
 * resolved; [SaveGroupPinUseCase.received] checks it again atomically with the save.
 */
class CheckIncomingGroupStateVersionUseCase(
    private val pinRepository: GroupPinRepository
) {
    /**
     * Whether a received pin of [messageId] (null clears the slot) replaces the stored one: it must be newer, and
     * an open expense board keeps the slot against other messages ([GroupPinSlotPolicy]).
     */
    suspend fun acceptsPin(groupId: String, changedAtEpochMilliseconds: Long, messageId: String?): Result<Boolean> =
        safeSuspendCall(logFailure = false) {
            val current = pinRepository.find(groupId).getOrThrow()
            GroupStateVersionPolicy.isNewer(changedAtEpochMilliseconds, current?.changedAtEpochMilliseconds ?: 0L) &&
                !GroupPinSlotPolicy.expensesKeepSlot(current, messageId)
        }
}
