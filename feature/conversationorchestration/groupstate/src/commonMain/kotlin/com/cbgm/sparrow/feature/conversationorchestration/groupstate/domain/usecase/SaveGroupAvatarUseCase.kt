package com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.usecase

import com.cbgm.sparrow.core.util.result.safeSuspendCall
import com.cbgm.sparrow.core.util.time.SystemClock
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.model.GroupAvatarState
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.model.GroupStateVersionPolicy
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.repository.GroupAvatarRepository

/** Saves a locally changed group avatar (null removes it) as a new version; the same image is published again. */
class SaveGroupAvatarUseCase(
    private val avatarRepository: GroupAvatarRepository
) {
    suspend operator fun invoke(groupId: String, bytes: ByteArray?): Result<GroupAvatarState> =
        safeSuspendCall(logFailure = false) {
            val previous = avatarRepository.get(groupId).getOrThrow()
            val saved =
                GroupAvatarState(
                    groupId = groupId,
                    bytes = bytes,
                    changedAtEpochMilliseconds = GroupStateVersionPolicy.nextChangedAt(
                        previousChangedAtEpochMilliseconds = previous.changedAtEpochMilliseconds,
                        nowEpochMilliseconds = SystemClock.nowEpochMilliseconds()
                    )
                )
            avatarRepository.save(saved).getOrThrow()
            saved
        }
}
