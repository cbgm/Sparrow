package com.cbgm.sparrow.feature.chats.domain.model.direct

import com.cbgm.sparrow.feature.chats.domain.model.MessageDeliveryEvent
import com.cbgm.sparrow.feature.chats.domain.model.MessageDeliveryStatus

object DirectMessageDeliveryStateMachine {
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
                if (current == MessageDeliveryStatus.SENDING) MessageDeliveryStatus.SENT else current

            MessageDeliveryEvent.SEND_FAILED ->
                when (current) {
                    MessageDeliveryStatus.QUEUED,
                    MessageDeliveryStatus.SENDING -> MessageDeliveryStatus.FAILED
                    else -> current
                }

            MessageDeliveryEvent.RETRY_REQUESTED ->
                if (current == MessageDeliveryStatus.FAILED) MessageDeliveryStatus.QUEUED else current

            MessageDeliveryEvent.DELIVERY_CONFIRMED ->
                when (current) {
                    MessageDeliveryStatus.QUEUED,
                    MessageDeliveryStatus.SENDING,
                    MessageDeliveryStatus.SENT,
                    MessageDeliveryStatus.FAILED -> MessageDeliveryStatus.DELIVERED
                    else -> current
                }

            MessageDeliveryEvent.DELIVERY_TIMED_OUT ->
                if (current == MessageDeliveryStatus.SENT) MessageDeliveryStatus.FAILED else current

            MessageDeliveryEvent.READ_CONFIRMED ->
                if (current == MessageDeliveryStatus.NOT_APPLICABLE) current else MessageDeliveryStatus.READ
        }

    fun canTransition(
        current: MessageDeliveryStatus,
        event: MessageDeliveryEvent
    ): Boolean = transition(current, event) != current
}
