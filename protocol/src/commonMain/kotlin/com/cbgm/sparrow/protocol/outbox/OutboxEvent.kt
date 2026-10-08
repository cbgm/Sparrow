package com.cbgm.sparrow.protocol.outbox

enum class OutboxEvent {
    PROCESSING_STARTED,
    SEND_SUCCEEDED,
    SEND_FAILED,
    DELIVERY_EXPIRED,
    RETRY_REQUESTED,
    RECOVERY_REQUESTED
}
