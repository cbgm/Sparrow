package com.cbgm.securechat.feature.chats.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MessageDeliveryStateMachineTest {
    @Test
    fun outgoingMessageFollowsExpectedTransitions() {
        val sending =
            MessageDeliveryStateMachine.transition(
                current = MessageDeliveryStatus.QUEUED,
                event = MessageDeliveryEvent.SEND_STARTED
            )
        val sent =
            MessageDeliveryStateMachine.transition(
                current = sending,
                event = MessageDeliveryEvent.SEND_SUCCEEDED
            )
        val delivered =
            MessageDeliveryStateMachine.transition(
                current = sent,
                event = MessageDeliveryEvent.DELIVERY_CONFIRMED
            )
        val read =
            MessageDeliveryStateMachine.transition(
                current = delivered,
                event = MessageDeliveryEvent.READ_CONFIRMED
            )

        assertEquals(MessageDeliveryStatus.SENDING, sending)
        assertEquals(MessageDeliveryStatus.SENT, sent)
        assertEquals(MessageDeliveryStatus.DELIVERED, delivered)
        assertEquals(MessageDeliveryStatus.READ, read)
    }

    @Test
    fun lateEventsNeverRegressReadState() {
        MessageDeliveryEvent.entries.forEach { event ->
            assertEquals(
                expected = MessageDeliveryStatus.READ,
                actual =
                    MessageDeliveryStateMachine.transition(
                        current = MessageDeliveryStatus.READ,
                        event = event
                    )
            )
        }
    }

    @Test
    fun failedMessageCanReturnToQueueAndBeSent() {
        val failed =
            MessageDeliveryStateMachine.transition(
                current = MessageDeliveryStatus.SENDING,
                event = MessageDeliveryEvent.SEND_FAILED
            )
        val queued =
            MessageDeliveryStateMachine.transition(
                current = failed,
                event = MessageDeliveryEvent.RETRY_REQUESTED
            )
        val sending =
            MessageDeliveryStateMachine.transition(
                current = queued,
                event = MessageDeliveryEvent.SEND_STARTED
            )

        assertEquals(MessageDeliveryStatus.FAILED, failed)
        assertEquals(MessageDeliveryStatus.QUEUED, queued)
        assertEquals(MessageDeliveryStatus.SENDING, sending)
    }

    @Test
    fun deliveryReceiptCanRecoverFromEarlierLocalFailure() {
        val delivered =
            MessageDeliveryStateMachine.transition(
                current = MessageDeliveryStatus.FAILED,
                event = MessageDeliveryEvent.DELIVERY_CONFIRMED
            )

        assertEquals(MessageDeliveryStatus.DELIVERED, delivered)
    }

    @Test
    fun incomingStatusIgnoresOutgoingEvents() {
        MessageDeliveryEvent.entries.forEach { event ->
            assertEquals(
                expected = MessageDeliveryStatus.NOT_APPLICABLE,
                actual =
                    MessageDeliveryStateMachine.transition(
                        current = MessageDeliveryStatus.NOT_APPLICABLE,
                        event = event
                    )
            )
        }
    }

    @Test
    fun canTransitionReportsOnlyRealStateChanges() {
        assertTrue(
            MessageDeliveryStateMachine.canTransition(
                current = MessageDeliveryStatus.FAILED,
                event = MessageDeliveryEvent.RETRY_REQUESTED
            )
        )
        assertFalse(
            MessageDeliveryStateMachine.canTransition(
                current = MessageDeliveryStatus.SENT,
                event = MessageDeliveryEvent.SEND_STARTED
            )
        )
    }

    @Test
    fun deliveryTimeoutMarksRelayAcceptedMessageAsFailed() {
        assertEquals(
            expected = MessageDeliveryStatus.FAILED,
            actual =
                MessageDeliveryStateMachine.transition(
                    current = MessageDeliveryStatus.SENT,
                    event = MessageDeliveryEvent.DELIVERY_TIMED_OUT
                )
        )
    }

    @Test
    fun groupStatusIsDerivedFromRecipientStates() {
        assertEquals(
            expected = MessageDeliveryStatus.SENT,
            actual =
                MessageDeliveryStateMachine.aggregate(
                    listOf(
                        MessageDeliveryStatus.SENT,
                        MessageDeliveryStatus.DELIVERED,
                        MessageDeliveryStatus.READ
                    )
                )
        )
        assertEquals(
            expected = MessageDeliveryStatus.DELIVERED,
            actual =
                MessageDeliveryStateMachine.aggregate(
                    listOf(
                        MessageDeliveryStatus.DELIVERED,
                        MessageDeliveryStatus.READ
                    )
                )
        )
        assertEquals(
            expected = MessageDeliveryStatus.READ,
            actual =
                MessageDeliveryStateMachine.aggregate(
                    listOf(
                        MessageDeliveryStatus.READ,
                        MessageDeliveryStatus.READ
                    )
                )
        )
    }

    @Test
    fun groupAggregationHandlesEmptyFailedAndMixedRecipientStates() {
        assertEquals(
            expected = MessageDeliveryStatus.NOT_APPLICABLE,
            actual = MessageDeliveryStateMachine.aggregate(emptyList())
        )
        assertEquals(
            expected = MessageDeliveryStatus.FAILED,
            actual =
                MessageDeliveryStateMachine.aggregate(
                    listOf(
                        MessageDeliveryStatus.FAILED,
                        MessageDeliveryStatus.FAILED
                    )
                )
        )
        assertEquals(
            expected = MessageDeliveryStatus.SENDING,
            actual =
                MessageDeliveryStateMachine.aggregate(
                    listOf(
                        MessageDeliveryStatus.SENDING,
                        MessageDeliveryStatus.SENT
                    )
                )
        )
        assertEquals(
            expected = MessageDeliveryStatus.FAILED,
            actual =
                MessageDeliveryStateMachine.aggregate(
                    listOf(
                        MessageDeliveryStatus.SENT,
                        MessageDeliveryStatus.FAILED
                    )
                )
        )
    }
}
