package com.cbgm.sparrow.feature.chats.domain.model

enum class MessageDeliveryEvent {
    SEND_STARTED,
    SEND_SUCCEEDED,
    SEND_FAILED,
    RETRY_REQUESTED,
    DELIVERY_CONFIRMED,
    DELIVERY_EXPIRED,
    READ_CONFIRMED
}
