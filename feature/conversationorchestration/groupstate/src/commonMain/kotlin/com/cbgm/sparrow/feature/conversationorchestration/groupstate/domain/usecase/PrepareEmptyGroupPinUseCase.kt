package com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.usecase

import com.cbgm.sparrow.core.util.result.safeSuspendCall
import com.cbgm.sparrow.core.util.time.SystemClock
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.model.GroupPinState
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.model.GroupStateVersionPolicy

/**
 * An empty pin slot as the version after [previous], so members replace their pin with nothing. Closing expenses
 * stores it only after its packets are queued; unpinning stores it as [unpublished] right away.
 */
class PrepareEmptyGroupPinUseCase {
    suspend operator fun invoke(groupId: String, previous: GroupPinState): Result<GroupPinState> =
        safeSuspendCall(logFailure = false) {
            GroupPinState.unpinned(
                groupId = groupId,
                changedAtEpochMilliseconds = GroupStateVersionPolicy.nextChangedAt(
                    previousChangedAtEpochMilliseconds = previous.changedAtEpochMilliseconds,
                    nowEpochMilliseconds = SystemClock.nowEpochMilliseconds()
                )
            )
        }

    /** The empty slot as a local change that is shown before its packets are queued. */
    suspend fun unpublished(groupId: String, previous: GroupPinState): Result<GroupPinState> =
        invoke(groupId, previous).map { pin -> pin.copy(publishPending = true) }
}
