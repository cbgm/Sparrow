package com.cbgm.sparrow.core.protocol.outbox

object OutboxStateMachine {
    /**
     * Returns null when an event is not valid for the current persisted state.
     */
    fun transition(
        current: OutboxStatus,
        event: OutboxEvent
    ): OutboxStatus? =
        when (event) {
            OutboxEvent.PROCESSING_STARTED ->
                when (current) {
                    OutboxStatus.PENDING,
                    OutboxStatus.FAILED -> OutboxStatus.PROCESSING

                    OutboxStatus.PROCESSING,
                    OutboxStatus.SENT -> null
                }

            OutboxEvent.SEND_SUCCEEDED ->
                if (current == OutboxStatus.PROCESSING) OutboxStatus.SENT else null

            OutboxEvent.SEND_FAILED ->
                if (current == OutboxStatus.PROCESSING) OutboxStatus.FAILED else null

            OutboxEvent.RETRY_REQUESTED ->
                if (current == OutboxStatus.FAILED) OutboxStatus.PENDING else null

            OutboxEvent.RECOVERY_REQUESTED ->
                if (current == OutboxStatus.PROCESSING) OutboxStatus.PENDING else null
        }

    fun requireTransition(
        current: OutboxStatus,
        event: OutboxEvent
    ): OutboxStatus =
        transition(current, event)
            ?: error("Invalid outbox transition: $current + $event")
}
