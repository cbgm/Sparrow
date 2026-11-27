package com.cbgm.sparrow.feature.chats.domain.model

enum class LocationShareState {
    IDLE,
    CAPTURING,
    AWAITING_SEND,
    SENDING;

    val isInProgress: Boolean
        get() = this != IDLE
}

enum class LocationShareEvent {
    CAPTURE_STARTED,
    LOCATION_CAPTURED,
    SEND_STARTED,
    COMPLETED,
    FAILED
}

object LocationShareStateMachine {
    fun transition(
        state: LocationShareState,
        event: LocationShareEvent
    ): LocationShareState =
        when (event) {
            LocationShareEvent.CAPTURE_STARTED -> state.advance(from = LocationShareState.IDLE, to = LocationShareState.CAPTURING)

            LocationShareEvent.LOCATION_CAPTURED ->
                state.advance(from = LocationShareState.CAPTURING, to = LocationShareState.AWAITING_SEND)

            LocationShareEvent.SEND_STARTED -> state.advance(from = LocationShareState.AWAITING_SEND, to = LocationShareState.SENDING)

            LocationShareEvent.COMPLETED,
            LocationShareEvent.FAILED -> LocationShareState.IDLE
        }

    /** Moves on to [to] only from [from]; any other state ignores the event. */
    private fun LocationShareState.advance(
        from: LocationShareState,
        to: LocationShareState
    ): LocationShareState = if (this == from) to else this
}
