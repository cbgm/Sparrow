package com.cbgm.securechat.feature.chats.domain.model

object MessageDeliveryStateMachine {
    /**
     * Returns the next state for an event.
     *
     * Late or duplicate transport events intentionally keep the current state.
     * This makes receipts idempotent and prevents DELIVERED or READ from
     * regressing when an older event arrives afterwards.
     */
    fun transition(
        current: MessageDeliveryStatus,
        event: MessageDeliveryEvent
    ): MessageDeliveryStatus =
        when (event) {
            MessageDeliveryEvent.SEND_STARTED ->
                when (current) {
                    MessageDeliveryStatus.QUEUED,
                    MessageDeliveryStatus.FAILED -> MessageDeliveryStatus.SENDING

                    else -> current
                }

            MessageDeliveryEvent.SEND_SUCCEEDED ->
                if (current == MessageDeliveryStatus.SENDING) {
                    MessageDeliveryStatus.SENT
                } else {
                    current
                }

            MessageDeliveryEvent.SEND_FAILED ->
                when (current) {
                    MessageDeliveryStatus.QUEUED,
                    MessageDeliveryStatus.SENDING -> MessageDeliveryStatus.FAILED

                    else -> current
                }

            MessageDeliveryEvent.RETRY_REQUESTED ->
                if (current == MessageDeliveryStatus.FAILED) {
                    MessageDeliveryStatus.QUEUED
                } else {
                    current
                }

            MessageDeliveryEvent.DELIVERY_CONFIRMED ->
                when (current) {
                    MessageDeliveryStatus.QUEUED,
                    MessageDeliveryStatus.SENDING,
                    MessageDeliveryStatus.SENT,
                    MessageDeliveryStatus.FAILED -> MessageDeliveryStatus.DELIVERED

                    else -> current
                }

            MessageDeliveryEvent.DELIVERY_TIMED_OUT ->
                if (current == MessageDeliveryStatus.SENT) {
                    MessageDeliveryStatus.FAILED
                } else {
                    current
                }

            MessageDeliveryEvent.READ_CONFIRMED ->
                if (current == MessageDeliveryStatus.NOT_APPLICABLE) {
                    current
                } else {
                    MessageDeliveryStatus.READ
                }
        }

    fun canTransition(
        current: MessageDeliveryStatus,
        event: MessageDeliveryEvent
    ): Boolean = transition(current, event) != current

    /**
     * A group reaches SENT, DELIVERED or READ only when every recipient has
     * reached that state or a later one.
     */
    fun aggregate(states: List<MessageDeliveryStatus>): MessageDeliveryStatus {
        if (states.isEmpty()) return MessageDeliveryStatus.NOT_APPLICABLE
        if (states.all { it == MessageDeliveryStatus.READ }) return MessageDeliveryStatus.READ
        if (states.all { it == MessageDeliveryStatus.DELIVERED || it == MessageDeliveryStatus.READ }) {
            return MessageDeliveryStatus.DELIVERED
        }
        if (states.any { it == MessageDeliveryStatus.SENDING }) return MessageDeliveryStatus.SENDING
        if (states.any { it == MessageDeliveryStatus.QUEUED }) return MessageDeliveryStatus.QUEUED
        if (states.any { it == MessageDeliveryStatus.FAILED }) return MessageDeliveryStatus.FAILED
        if (states.all { it == MessageDeliveryStatus.SENT || it == MessageDeliveryStatus.DELIVERED || it == MessageDeliveryStatus.READ }) {
            return MessageDeliveryStatus.SENT
        }
        return MessageDeliveryStatus.QUEUED
    }
}
