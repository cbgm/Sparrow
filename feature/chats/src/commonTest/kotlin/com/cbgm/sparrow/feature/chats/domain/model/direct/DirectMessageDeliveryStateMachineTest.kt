package com.cbgm.sparrow.feature.chats.domain.model.direct

import com.cbgm.sparrow.feature.chats.domain.model.MessageDeliveryEvent
import com.cbgm.sparrow.feature.chats.domain.model.MessageDeliveryStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DirectMessageDeliveryStateMachineTest {
    @Test
    fun waitingForAuthorizationMovesToQueueOnlyAfterAuthorizationIsGranted() {
        assertFalse(
            DirectMessageDeliveryStateMachine.canTransition(
                MessageDeliveryStatus.WAITING_FOR_AUTHORIZATION,
                MessageDeliveryEvent.SEND_STARTED
            )
        )
        assertEquals(
            MessageDeliveryStatus.QUEUED,
            transition(
                MessageDeliveryStatus.WAITING_FOR_AUTHORIZATION,
                MessageDeliveryEvent.AUTHORIZATION_GRANTED
            )
        )
    }

    @Test
    fun outgoingMessageFollowsExpectedTransitions() {
        val sending = transition(MessageDeliveryStatus.QUEUED, MessageDeliveryEvent.SEND_STARTED)
        val sent = transition(sending, MessageDeliveryEvent.SEND_SUCCEEDED)
        val delivered = transition(sent, MessageDeliveryEvent.DELIVERY_CONFIRMED)
        val read = transition(delivered, MessageDeliveryEvent.READ_CONFIRMED)

        assertEquals(MessageDeliveryStatus.SENDING, sending)
        assertEquals(MessageDeliveryStatus.SENT, sent)
        assertEquals(MessageDeliveryStatus.DELIVERED, delivered)
        assertEquals(MessageDeliveryStatus.READ, read)
    }

    @Test
    fun lateEventsNeverRegressReadState() {
        MessageDeliveryEvent.entries.forEach { event ->
            assertEquals(MessageDeliveryStatus.READ, transition(MessageDeliveryStatus.READ, event))
        }
    }

    @Test
    fun failedMessageCanReturnToQueue() {
        val failed = transition(MessageDeliveryStatus.SENDING, MessageDeliveryEvent.SEND_FAILED)
        val queued = transition(failed, MessageDeliveryEvent.RETRY_REQUESTED)

        assertEquals(MessageDeliveryStatus.FAILED, failed)
        assertEquals(MessageDeliveryStatus.QUEUED, queued)
        assertTrue(DirectMessageDeliveryStateMachine.canTransition(failed, MessageDeliveryEvent.RETRY_REQUESTED))
        assertFalse(DirectMessageDeliveryStateMachine.canTransition(MessageDeliveryStatus.SENT, MessageDeliveryEvent.SEND_STARTED))
    }

    @Test
    fun serverAcceptedMessageFailsOnlyWhenServerRetentionExpires() {
        val sent = transition(MessageDeliveryStatus.SENDING, MessageDeliveryEvent.SEND_SUCCEEDED)

        assertEquals(MessageDeliveryStatus.SENT, sent)
        assertEquals(
            MessageDeliveryStatus.FAILED,
            transition(sent, MessageDeliveryEvent.DELIVERY_EXPIRED)
        )
    }

    @Test
    fun deliveryReceiptCanRecoverFromEarlierFailure() {
        assertEquals(
            MessageDeliveryStatus.DELIVERED,
            transition(MessageDeliveryStatus.FAILED, MessageDeliveryEvent.DELIVERY_CONFIRMED)
        )
    }

    private fun transition(
        current: MessageDeliveryStatus,
        event: MessageDeliveryEvent
    ): MessageDeliveryStatus = DirectMessageDeliveryStateMachine.transition(current, event)
}
