package com.cbgm.sparrow.feature.messaging.domain.model

/** Server acceptance does not mean delivery to the recipient. */
enum class MessagingTransportState {
    ACCEPTED_BY_RELAY,
    FAILED,
    EXPIRED
}

/**
 * Persisted current-state snapshot of one protocol outbox row.
 * Replayed on subscription and on database changes; NOT an exactly-once event.
 * Consumers must deduplicate by packetId, attemptCount and state.
 */
data class MessagingTransportResult(
    val packetId: String,
    val attemptCount: Int,
    val state: MessagingTransportState,
    val errorMessage: String?,
    val expiresAtEpochMilliseconds: Long?,
    val updatedAtEpochMilliseconds: Long
)
