package com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.usecase

import com.cbgm.sparrow.core.util.result.safeSuspendCall
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.model.GroupPinState
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.model.GroupStateVersionPolicy
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.repository.GroupPinRepository

/** Stores a pin slot version unless a newer one arrived meanwhile ([GroupStateVersionPolicy]). */
class SaveGroupPinUseCase(
    private val pinRepository: GroupPinRepository
) {
    suspend operator fun invoke(pin: GroupPinState): Result<Unit> =
        safeSuspendCall(logFailure = false) {
            val currentChangedAt = pinRepository.find(pin.groupId).getOrThrow()?.changedAtEpochMilliseconds ?: 0L
            if (GroupStateVersionPolicy.isNewer(pin.changedAtEpochMilliseconds, currentChangedAt)) {
                pinRepository.save(pin).getOrThrow()
            }
        }
}
