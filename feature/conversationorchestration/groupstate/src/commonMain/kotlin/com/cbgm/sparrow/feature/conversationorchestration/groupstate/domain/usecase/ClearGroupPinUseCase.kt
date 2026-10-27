package com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.usecase

import com.cbgm.sparrow.core.util.result.safeSuspendCall
import com.cbgm.sparrow.core.util.time.SystemClock
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.model.GroupPinState
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.model.GroupStateVersionPolicy
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.repository.GroupPinRepository

/** Saves an empty pin slot as the version after [previous], so members replace their pin with nothing. */
class ClearGroupPinUseCase(
    private val pinRepository: GroupPinRepository
) {
    suspend operator fun invoke(groupId: String, previous: GroupPinState): Result<GroupPinState> =
        safeSuspendCall(logFailure = false) {
            val emptyPin =
                GroupPinState.unpinned(
                    groupId = groupId,
                    changedAtEpochMilliseconds = GroupStateVersionPolicy.nextChangedAt(
                        previousChangedAtEpochMilliseconds = previous.changedAtEpochMilliseconds,
                        nowEpochMilliseconds = SystemClock.nowEpochMilliseconds()
                    )
                )
            pinRepository.save(emptyPin).getOrThrow()
            emptyPin
        }
}
