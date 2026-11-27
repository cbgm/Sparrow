package com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.usecase

import com.cbgm.sparrow.core.util.result.safeSuspendCall
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.model.GroupStateVersionPolicy
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.model.GroupTitleState
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.repository.GroupTitleRepository

/**
 * Stores a local or received title version unless a newer one is stored ([GroupStateVersionPolicy]). The save is
 * a compare-and-set on the version that was checked, so a concurrent save is never overwritten by an older
 * version; when another save wins the race, the check runs again. Returns whether [state] was stored.
 */
class SaveGroupTitleUseCase(
    private val titleRepository: GroupTitleRepository
) {
    suspend operator fun invoke(state: GroupTitleState): Result<Boolean> =
        safeSuspendCall(logFailure = false) { save(state) }

    private suspend fun save(state: GroupTitleState): Boolean {
        while (true) {
            val currentChangedAt = titleRepository.get(state.groupId).getOrThrow().changedAtEpochMilliseconds
            if (!GroupStateVersionPolicy.isNewer(state.changedAtEpochMilliseconds, currentChangedAt)) return false
            if (titleRepository.saveIfVersion(state, currentChangedAt).getOrThrow()) return true
        }
    }
}
