package com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.model

/** What happened when a message was pinned locally. */
sealed interface GroupPinSaveResult {
    data class Saved(
        val pin: GroupPinState
    ) : GroupPinSaveResult

    /** The message is pinned already; nothing was saved. */
    data object AlreadyPinned : GroupPinSaveResult

    /** An open expense board holds the slot; nothing was saved. */
    data object SlotHeldByExpenses : GroupPinSaveResult
}
