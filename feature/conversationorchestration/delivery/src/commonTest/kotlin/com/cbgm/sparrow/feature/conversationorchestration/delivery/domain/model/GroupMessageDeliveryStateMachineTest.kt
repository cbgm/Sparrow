package com.cbgm.sparrow.feature.conversationorchestration.delivery.domain.model

import com.cbgm.sparrow.core.base.message.domain.model.MessageDeliveryStatus
import kotlin.test.Test
import kotlin.test.assertEquals

class GroupMessageDeliveryStateMachineTest {
    @Test
    fun temporaryTransportFailureReturnsToQueueInsteadOfTerminalFailure() {
        val started = GroupMessageDeliveryStateMachine.transition(
            MessageDeliveryStatus.QUEUED,
            MessageDeliveryEvent.SEND_STARTED
        )
        assertEquals(
            MessageDeliveryStatus.QUEUED,
            GroupMessageDeliveryStateMachine.transition(started, MessageDeliveryEvent.TRANSPORT_RETRY_PENDING)
        )
        assertEquals(
            MessageDeliveryStatus.FAILED,
            GroupMessageDeliveryStateMachine.transition(started, MessageDeliveryEvent.SEND_FAILED)
        )
    }

    @Test
    fun serverAcceptedRecipientFailsOnlyWhenServerRetentionExpires() {
        val sent =
            GroupMessageDeliveryStateMachine.transition(
                MessageDeliveryStatus.SENDING,
                MessageDeliveryEvent.SEND_SUCCEEDED
            )

        assertEquals(MessageDeliveryStatus.SENT, sent)
        assertEquals(
            MessageDeliveryStatus.FAILED,
            GroupMessageDeliveryStateMachine.transition(
                sent,
                MessageDeliveryEvent.DELIVERY_EXPIRED
            )
        )
    }
}
