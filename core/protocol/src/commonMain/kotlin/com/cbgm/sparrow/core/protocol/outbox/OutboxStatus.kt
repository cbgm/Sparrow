package com.cbgm.sparrow.core.protocol.outbox

enum class OutboxStatus {
    PENDING,
    PROCESSING,
    SENT,
    FAILED,
    EXPIRED
}
