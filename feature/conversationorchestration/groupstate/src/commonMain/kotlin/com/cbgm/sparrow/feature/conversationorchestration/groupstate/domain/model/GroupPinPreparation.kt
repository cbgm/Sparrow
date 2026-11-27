package com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.model

/** What pinning a message locally would change; nothing is saved before the pin is queued for the members. */
sealed interface GroupPinPreparation {
    data class Ready(
        val pin: GroupPinState
    ) : GroupPinPreparation

    /** The message is pinned already. */
    data object AlreadyPinned : GroupPinPreparation

    /** An open expense board holds the slot. */
    data object SlotHeldByExpenses : GroupPinPreparation
}
