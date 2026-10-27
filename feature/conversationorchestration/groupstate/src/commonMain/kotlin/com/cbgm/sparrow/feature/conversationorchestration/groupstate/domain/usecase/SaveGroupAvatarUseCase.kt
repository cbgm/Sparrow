package com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.usecase

import com.cbgm.sparrow.core.util.result.safeSuspendCall
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.model.GroupAvatarState
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.model.GroupStateVersionPolicy
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.repository.GroupAvatarRepository

/** Stores a local avatar version unless a newer one arrived meanwhile ([GroupStateVersionPolicy]). */
class SaveGroupAvatarUseCase(
    private val avatarRepository: GroupAvatarRepository
) {
    suspend operator fun invoke(state: GroupAvatarState): Result<Unit> =
        safeSuspendCall(logFailure = false) {
            val current = avatarRepository.get(state.groupId).getOrThrow()
            if (GroupStateVersionPolicy.isNewer(state.changedAtEpochMilliseconds, current.changedAtEpochMilliseconds)) {
                avatarRepository.save(state).getOrThrow()
            }
        }
}
