package com.cbgm.sparrow.protocol.outbox

/** Immutable failed transport attempt, persisted independently of the mutable outbox row. */
data class ProtocolOutboxFailureEvent(
    val eventId: String,
    val packetId: String,
    val encodedPacket: ByteArray,
    val attemptCount: Int,
    val errorMessage: String,
    val occurredAtEpochMilliseconds: Long
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ProtocolOutboxFailureEvent) return false

        return eventId == other.eventId &&
            packetId == other.packetId &&
            encodedPacket.contentEquals(other.encodedPacket) &&
            attemptCount == other.attemptCount &&
            errorMessage == other.errorMessage &&
            occurredAtEpochMilliseconds == other.occurredAtEpochMilliseconds
    }

    override fun hashCode(): Int {
        var result = eventId.hashCode()
        result = 31 * result + packetId.hashCode()
        result = 31 * result + encodedPacket.contentHashCode()
        result = 31 * result + attemptCount
        result = 31 * result + errorMessage.hashCode()
        result = 31 * result + occurredAtEpochMilliseconds.hashCode()
        return result
    }
}
