package com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.usecase

import com.cbgm.sparrow.core.util.result.safeSuspendCall
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.model.GroupDescriptionState
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.model.GroupStateVersionPolicy
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.repository.GroupDescriptionRepository

/** Stores a local description version unless a newer one arrived meanwhile ([GroupStateVersionPolicy]). */
class SaveGroupDescriptionUseCase(
    private val descriptionRepository: GroupDescriptionRepository
) {
    suspend operator fun invoke(state: GroupDescriptionState): Result<Unit> =
        safeSuspendCall(logFailure = false) {
            val current = descriptionRepository.get(state.groupId).getOrThrow()
            if (GroupStateVersionPolicy.isNewer(state.changedAtEpochMilliseconds, current.changedAtEpochMilliseconds)) {
                descriptionRepository.save(state).getOrThrow()
            }
        }
}
