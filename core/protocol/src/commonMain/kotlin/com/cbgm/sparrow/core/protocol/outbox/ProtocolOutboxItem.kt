package com.cbgm.sparrow.core.protocol.outbox

data class ProtocolOutboxItem(
    val id: String,
    val contactId: String,
    val packetId: String,
    val encodedPacket: ByteArray,
    val status: OutboxStatus,
    val attemptCount: Int,
    val lastError: String?,
    val expiresAtEpochMilliseconds: Long? = null,
    val createdAtEpochMilliseconds: Long,
    val updatedAtEpochMilliseconds: Long
) {
    init {
        require(id.isNotBlank()) {
            "Outbox item ID must not be blank"
        }

        require(contactId.isNotBlank()) {
            "Contact ID must not be blank"
        }

        require(packetId.isNotBlank()) {
            "Packet ID must not be blank"
        }

        require(encodedPacket.isNotEmpty()) {
            "Encoded packet must not be empty"
        }

        require(attemptCount >= 0) {
            "Attempt count must not be negative"
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }

        if (other !is ProtocolOutboxItem) return false

        return id == other.id &&
            contactId == other.contactId &&
            packetId == other.packetId &&
            encodedPacket.contentEquals(
                other.encodedPacket
            ) &&
            status == other.status &&
            attemptCount == other.attemptCount &&
            lastError == other.lastError &&
            expiresAtEpochMilliseconds == other.expiresAtEpochMilliseconds &&
            createdAtEpochMilliseconds ==
            other.createdAtEpochMilliseconds &&
            updatedAtEpochMilliseconds ==
            other.updatedAtEpochMilliseconds
    }

    override fun hashCode(): Int {
        var result = id.hashCode()

        result = 31 * result + contactId.hashCode()

        result = 31 * result + packetId.hashCode()

        result = 31 * result + encodedPacket.contentHashCode()

        result = 31 * result + status.hashCode()

        result = 31 * result + attemptCount

        result = 31 * result + (lastError?.hashCode() ?: 0)

        result = 31 * result + (expiresAtEpochMilliseconds?.hashCode() ?: 0)

        result = 31 * result + createdAtEpochMilliseconds.hashCode()

        result = 31 * result + updatedAtEpochMilliseconds.hashCode()

        return result
    }
}
