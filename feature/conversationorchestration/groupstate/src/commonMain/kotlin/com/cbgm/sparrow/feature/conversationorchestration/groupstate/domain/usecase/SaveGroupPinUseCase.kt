package com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.usecase

import com.cbgm.sparrow.core.util.result.safeSuspendCall
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.model.GroupPinSlotPolicy
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.model.GroupPinState
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.model.GroupStateVersionPolicy
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.repository.GroupPinRepository

/**
 * Stores a pin slot version unless a newer one is stored ([GroupStateVersionPolicy]). The save is a
 * compare-and-set on the version that was checked, so a concurrent save is never overwritten by an older
 * version; when another save wins the race, the check runs again. Both functions return whether the pin was stored.
 */
class SaveGroupPinUseCase(
    private val pinRepository: GroupPinRepository
) {
    /** A local pin: the slot rule was already checked while preparing it. */
    suspend operator fun invoke(pin: GroupPinState): Result<Boolean> =
        safeSuspendCall(logFailure = false) { save(pin) { true } }

    /** A received pin: an open expense board also keeps the slot against other messages ([GroupPinSlotPolicy]). */
    suspend fun received(pin: GroupPinState): Result<Boolean> =
        safeSuspendCall(logFailure = false) {
            save(pin) { current -> !GroupPinSlotPolicy.expensesKeepSlot(current, pin.messageId) }
        }

    private suspend inline fun save(pin: GroupPinState, slotAccepts: (GroupPinState?) -> Boolean): Boolean {
        while (true) {
            val current = pinRepository.find(pin.groupId).getOrThrow()
            val currentChangedAt = current?.changedAtEpochMilliseconds ?: 0L
            if (!GroupStateVersionPolicy.isNewer(pin.changedAtEpochMilliseconds, currentChangedAt)) return false
            if (!slotAccepts(current)) return false
            if (pinRepository.saveIfVersion(pin, currentChangedAt).getOrThrow()) return true
        }
    }
}
