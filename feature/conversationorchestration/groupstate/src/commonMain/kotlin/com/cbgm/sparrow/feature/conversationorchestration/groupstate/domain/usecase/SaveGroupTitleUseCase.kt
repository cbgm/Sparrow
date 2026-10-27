package com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.usecase

import com.cbgm.sparrow.core.util.result.safeSuspendCall
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.model.GroupStateVersionPolicy
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.model.GroupTitleState
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.repository.GroupTitleRepository

/** Stores a local title version unless a newer one arrived meanwhile ([GroupStateVersionPolicy]). */
class SaveGroupTitleUseCase(
    private val titleRepository: GroupTitleRepository
) {
    suspend operator fun invoke(state: GroupTitleState): Result<Unit> =
        safeSuspendCall(logFailure = false) {
            val current = titleRepository.get(state.groupId).getOrThrow()
            if (GroupStateVersionPolicy.isNewer(state.changedAtEpochMilliseconds, current.changedAtEpochMilliseconds)) {
                titleRepository.save(state).getOrThrow()
            }
        }
}
