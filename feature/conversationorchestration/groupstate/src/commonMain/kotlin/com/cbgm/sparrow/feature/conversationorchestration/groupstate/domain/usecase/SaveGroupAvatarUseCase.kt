package com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.usecase

import com.cbgm.sparrow.core.util.result.safeSuspendCall
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.model.GroupAvatarState
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.model.GroupStateVersionPolicy
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.repository.GroupAvatarRepository

/**
 * Stores a local or received avatar version unless a newer one is stored ([GroupStateVersionPolicy]). The save is
 * a compare-and-set on the version that was checked, so a concurrent save is never overwritten by an older
 * version; when another save wins the race, the check runs again. Returns whether [state] was stored.
 */
class SaveGroupAvatarUseCase(
    private val avatarRepository: GroupAvatarRepository
) {
    suspend operator fun invoke(state: GroupAvatarState): Result<Boolean> =
        safeSuspendCall(logFailure = false) { save(state) }

    private suspend fun save(state: GroupAvatarState): Boolean {
        while (true) {
            val currentChangedAt = avatarRepository.get(state.groupId).getOrThrow().changedAtEpochMilliseconds
            if (!GroupStateVersionPolicy.isNewer(state.changedAtEpochMilliseconds, currentChangedAt)) return false
            if (avatarRepository.saveIfVersion(state, currentChangedAt).getOrThrow()) return true
        }
    }
}
