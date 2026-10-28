package com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.usecase

import com.cbgm.sparrow.core.util.result.safeSuspendCall
import com.cbgm.sparrow.core.util.time.SystemClock
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.model.GroupAvatarState
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.model.GroupStateVersionPolicy
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.repository.GroupAvatarRepository

/**
 * The next, still unpublished version of a locally changed group avatar (null bytes remove it); the same image is
 * published again.
 */
class PrepareGroupAvatarUseCase(
    private val avatarRepository: GroupAvatarRepository
) {
    suspend operator fun invoke(groupId: String, bytes: ByteArray?): Result<GroupAvatarState> =
        safeSuspendCall(logFailure = false) {
            val previous = avatarRepository.get(groupId).getOrThrow()
            GroupAvatarState(
                groupId = groupId,
                bytes = bytes,
                changedAtEpochMilliseconds = GroupStateVersionPolicy.nextChangedAt(
                    previousChangedAtEpochMilliseconds = previous.changedAtEpochMilliseconds,
                    nowEpochMilliseconds = SystemClock.nowEpochMilliseconds()
                ),
                publishPending = true
            )
        }
}
