package com.cbgm.sparrow.feature.chats.domain.model.group

import com.cbgm.sparrow.feature.chats.domain.model.MessageDeliveryStatus
import kotlin.test.Test
import kotlin.test.assertEquals

class GroupMessageDeliveryStateMachineTest {
    @Test
    fun groupStatusIsDerivedFromRecipientStates() {
        assertAggregate(MessageDeliveryStatus.SENT, MessageDeliveryStatus.SENT, MessageDeliveryStatus.DELIVERED, MessageDeliveryStatus.READ)
        assertAggregate(MessageDeliveryStatus.DELIVERED, MessageDeliveryStatus.DELIVERED, MessageDeliveryStatus.READ)
        assertAggregate(MessageDeliveryStatus.READ, MessageDeliveryStatus.READ, MessageDeliveryStatus.READ)
    }

    @Test
    fun groupAggregationHandlesEmptyFailedAndMixedStates() {
        assertEquals(MessageDeliveryStatus.NOT_APPLICABLE, GroupMessageDeliveryStateMachine.aggregate(emptyList()))
        assertAggregate(MessageDeliveryStatus.FAILED, MessageDeliveryStatus.FAILED, MessageDeliveryStatus.FAILED)
        assertAggregate(MessageDeliveryStatus.SENDING, MessageDeliveryStatus.SENDING, MessageDeliveryStatus.SENT)
        assertAggregate(MessageDeliveryStatus.FAILED, MessageDeliveryStatus.SENT, MessageDeliveryStatus.FAILED)
    }

    private fun assertAggregate(
        expected: MessageDeliveryStatus,
        vararg statuses: MessageDeliveryStatus
    ) {
        assertEquals(expected, GroupMessageDeliveryStateMachine.aggregate(statuses.toList()))
    }
}
