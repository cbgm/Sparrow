package com.cbgm.sparrow.feature.chats.domain.model

enum class MessageDeliveryEvent {
    AUTHORIZATION_GRANTED,
    SEND_STARTED,
    SEND_SUCCEEDED,
    SEND_FAILED,

    /** The relay send failed temporarily; the same authorized packet is still queued for retry. */
    TRANSPORT_RETRY_PENDING,
    RETRY_REQUESTED,
    DELIVERY_CONFIRMED,
    DELIVERY_EXPIRED,
    READ_CONFIRMED
}
