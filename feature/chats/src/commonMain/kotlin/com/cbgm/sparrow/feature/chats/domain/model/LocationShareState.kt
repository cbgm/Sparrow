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
            LocationShareEvent.CAPTURE_STARTED ->
                if (state == LocationShareState.IDLE) LocationShareState.CAPTURING else state

            LocationShareEvent.LOCATION_CAPTURED ->
                if (state == LocationShareState.CAPTURING) LocationShareState.AWAITING_SEND else state

            LocationShareEvent.SEND_STARTED ->
                if (state == LocationShareState.AWAITING_SEND) LocationShareState.SENDING else state

            LocationShareEvent.COMPLETED,
            LocationShareEvent.FAILED -> LocationShareState.IDLE
        }
}
