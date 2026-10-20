package com.cbgm.sparrow.core.base.message.domain.model

/** Combines the per-recipient delivery states of one group message into the status shown for it. */
object GroupDeliveryStatusAggregation {
    fun aggregate(states: List<MessageDeliveryStatus>): MessageDeliveryStatus {
        if (states.isEmpty()) return MessageDeliveryStatus.NOT_APPLICABLE
        if (states.any { it == MessageDeliveryStatus.READ }) return MessageDeliveryStatus.READ
        if (states.all { it == MessageDeliveryStatus.DELIVERED }) {
            return MessageDeliveryStatus.DELIVERED
        }
        if (states.any { it == MessageDeliveryStatus.SENDING }) return MessageDeliveryStatus.SENDING
        if (states.any { it == MessageDeliveryStatus.QUEUED }) return MessageDeliveryStatus.QUEUED
        if (states.any { it == MessageDeliveryStatus.FAILED }) return MessageDeliveryStatus.FAILED
        if (
            states.all {
                it == MessageDeliveryStatus.SENT || it == MessageDeliveryStatus.DELIVERED || it == MessageDeliveryStatus.READ
            }
        ) {
            return MessageDeliveryStatus.SENT
        }
        return MessageDeliveryStatus.QUEUED
    }
}
