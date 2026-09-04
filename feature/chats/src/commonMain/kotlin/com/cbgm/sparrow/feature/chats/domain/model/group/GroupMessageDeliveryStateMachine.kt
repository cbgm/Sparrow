package com.cbgm.sparrow.feature.chats.domain.model.group

import com.cbgm.sparrow.feature.chats.domain.model.MessageDeliveryEvent
import com.cbgm.sparrow.feature.chats.domain.model.MessageDeliveryStatus

object GroupMessageDeliveryStateMachine {
    fun transition(
        current: MessageDeliveryStatus,
        event: MessageDeliveryEvent
    ): MessageDeliveryStatus =
        when (current to event) {
            MessageDeliveryStatus.QUEUED to MessageDeliveryEvent.SEND_STARTED,
            MessageDeliveryStatus.FAILED to MessageDeliveryEvent.SEND_STARTED -> MessageDeliveryStatus.SENDING

            MessageDeliveryStatus.SENDING to MessageDeliveryEvent.SEND_SUCCEEDED -> MessageDeliveryStatus.SENT

            MessageDeliveryStatus.QUEUED to MessageDeliveryEvent.SEND_FAILED,
            MessageDeliveryStatus.SENDING to MessageDeliveryEvent.SEND_FAILED -> MessageDeliveryStatus.FAILED

            MessageDeliveryStatus.FAILED to MessageDeliveryEvent.RETRY_REQUESTED -> MessageDeliveryStatus.QUEUED

            MessageDeliveryStatus.QUEUED to MessageDeliveryEvent.DELIVERY_CONFIRMED,
            MessageDeliveryStatus.SENDING to MessageDeliveryEvent.DELIVERY_CONFIRMED,
            MessageDeliveryStatus.SENT to MessageDeliveryEvent.DELIVERY_CONFIRMED,
            MessageDeliveryStatus.FAILED to MessageDeliveryEvent.DELIVERY_CONFIRMED -> MessageDeliveryStatus.DELIVERED

            MessageDeliveryStatus.SENT to MessageDeliveryEvent.DELIVERY_EXPIRED -> MessageDeliveryStatus.FAILED

            MessageDeliveryStatus.QUEUED to MessageDeliveryEvent.READ_CONFIRMED,
            MessageDeliveryStatus.SENDING to MessageDeliveryEvent.READ_CONFIRMED,
            MessageDeliveryStatus.SENT to MessageDeliveryEvent.READ_CONFIRMED,
            MessageDeliveryStatus.FAILED to MessageDeliveryEvent.READ_CONFIRMED,
            MessageDeliveryStatus.DELIVERED to MessageDeliveryEvent.READ_CONFIRMED,
            MessageDeliveryStatus.READ to MessageDeliveryEvent.READ_CONFIRMED -> MessageDeliveryStatus.READ

            else -> current
        }

    fun canTransition(
        current: MessageDeliveryStatus,
        event: MessageDeliveryEvent
    ): Boolean = transition(current, event) != current

    fun aggregate(states: List<MessageDeliveryStatus>): MessageDeliveryStatus {
        if (states.isEmpty()) return MessageDeliveryStatus.NOT_APPLICABLE
        if (states.any { it == MessageDeliveryStatus.READ }) return MessageDeliveryStatus.READ
        if (states.all { it == MessageDeliveryStatus.DELIVERED }) {
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
