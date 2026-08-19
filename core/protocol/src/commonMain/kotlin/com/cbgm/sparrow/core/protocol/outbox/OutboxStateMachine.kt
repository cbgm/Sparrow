package com.cbgm.sparrow.core.protocol.outbox

object OutboxStateMachine {
    /** Returns null when an event is not valid for the current persisted state. */
    fun transition(
        current: OutboxStatus,
        event: OutboxEvent
    ): OutboxStatus? =
        when (current to event) {
            OutboxStatus.PENDING to OutboxEvent.PROCESSING_STARTED,
            OutboxStatus.FAILED to OutboxEvent.PROCESSING_STARTED -> OutboxStatus.PROCESSING

            OutboxStatus.PROCESSING to OutboxEvent.SEND_SUCCEEDED -> OutboxStatus.SENT
            OutboxStatus.PROCESSING to OutboxEvent.SEND_FAILED -> OutboxStatus.FAILED

            OutboxStatus.SENT to OutboxEvent.DELIVERY_EXPIRED -> OutboxStatus.EXPIRED

            OutboxStatus.FAILED to OutboxEvent.RETRY_REQUESTED,
            OutboxStatus.EXPIRED to OutboxEvent.RETRY_REQUESTED -> OutboxStatus.PENDING

            OutboxStatus.PROCESSING to OutboxEvent.RECOVERY_REQUESTED -> OutboxStatus.PENDING

            else -> null
        }

    fun requireTransition(
        current: OutboxStatus,
        event: OutboxEvent
    ): OutboxStatus =
        transition(current, event)
            ?: error("Invalid outbox transition: $current + $event")
}
