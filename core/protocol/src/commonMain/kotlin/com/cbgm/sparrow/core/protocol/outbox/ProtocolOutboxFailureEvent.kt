package com.cbgm.sparrow.core.protocol.outbox

/** Immutable failed transport attempt, persisted independently of the mutable outbox row. */
data class ProtocolOutboxFailureEvent(
    val eventId: String,
    val packetId: String,
    val encodedPacket: ByteArray,
    val attemptCount: Int,
    val errorMessage: String,
    val occurredAtEpochMilliseconds: Long
)
