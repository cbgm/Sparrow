package com.cbgm.sparrow.feature.messaging.domain.model

/** Generic transport failure recorded for a specific attempt, regardless of later retry state. */
data class MessagingFailureEvent(
    val eventId: String,
    val packetId: String,
    val encodedPacket: ByteArray,
    val attemptCount: Int,
    val errorMessage: String,
    val occurredAtEpochMilliseconds: Long
)
