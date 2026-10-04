package com.cbgm.sparrow.protocol.outbox

enum class OutboxStatus {
    PENDING,
    PROCESSING,
    SENT,
    FAILED,
    EXPIRED,

    /** Historical recipient identity is retired; this packet must never be retried as-is. */
    QUARANTINED
}
