package com.cbgm.sparrow.core.base.message.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals

class GroupDeliveryStatusAggregationTest {
    @Test
    fun groupStatusIsDerivedFromRecipientStates() {
        assertAggregate(MessageDeliveryStatus.SENT, MessageDeliveryStatus.SENT, MessageDeliveryStatus.DELIVERED)
        assertAggregate(
            MessageDeliveryStatus.DELIVERED,
            MessageDeliveryStatus.DELIVERED,
            MessageDeliveryStatus.DELIVERED
        )
        assertAggregate(
            MessageDeliveryStatus.READ,
            MessageDeliveryStatus.SENT,
            MessageDeliveryStatus.DELIVERED,
            MessageDeliveryStatus.READ
        )
        assertAggregate(MessageDeliveryStatus.READ, MessageDeliveryStatus.DELIVERED, MessageDeliveryStatus.READ)
        assertAggregate(MessageDeliveryStatus.READ, MessageDeliveryStatus.READ, MessageDeliveryStatus.READ)
    }

    @Test
    fun groupAggregationHandlesEmptyFailedAndMixedStates() {
        assertEquals(MessageDeliveryStatus.NOT_APPLICABLE, GroupDeliveryStatusAggregation.aggregate(emptyList()))
        assertAggregate(MessageDeliveryStatus.FAILED, MessageDeliveryStatus.FAILED, MessageDeliveryStatus.FAILED)
        assertAggregate(MessageDeliveryStatus.SENDING, MessageDeliveryStatus.SENDING, MessageDeliveryStatus.SENT)
        assertAggregate(MessageDeliveryStatus.FAILED, MessageDeliveryStatus.SENT, MessageDeliveryStatus.FAILED)
    }

    private fun assertAggregate(
        expected: MessageDeliveryStatus,
        vararg statuses: MessageDeliveryStatus
    ) {
        assertEquals(expected, GroupDeliveryStatusAggregation.aggregate(statuses.toList()))
    }
}
