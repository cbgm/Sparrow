package com.cbgm.sparrow.data.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "protocol_outbox",
    foreignKeys = [
        ForeignKey(
            entity = ContactEntity::class,
            parentColumns = ["id"],
            childColumns = ["contactId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["contactId"]),

        Index(
            value = ["packetId"],
            unique = true
        ),

        Index(
            value = [
                "status",
                "createdAtEpochMilliseconds"
            ]
        )
    ]
)
data class ProtocolOutboxEntity(
    @PrimaryKey
    val id: String,
    val contactId: String,
    /**
     * Unique protocol packet ID.
     */
    val packetId: String,
    /**
     * Packet encoded by PacketCodec.
     *
     * This is not yet transport-encrypted.
     */
    val encodedPacket: ByteArray,
    /**
     * OutboxStatus enum name.
     */
    val status: String,
    val attemptCount: Int,
    val lastError: String?,
    val createdAtEpochMilliseconds: Long,
    val updatedAtEpochMilliseconds: Long
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true

        if (other !is ProtocolOutboxEntity) return false

        return id == other.id &&
            contactId == other.contactId &&
            packetId == other.packetId &&
            encodedPacket.contentEquals(other.encodedPacket) &&
            status == other.status &&
            attemptCount == other.attemptCount &&
            lastError == other.lastError &&
            createdAtEpochMilliseconds == other.createdAtEpochMilliseconds &&
            updatedAtEpochMilliseconds == other.updatedAtEpochMilliseconds
    }

    override fun hashCode(): Int {
        var result = id.hashCode()

        result = 31 * result + contactId.hashCode()

        result = 31 * result + packetId.hashCode()

        result = 31 * result + encodedPacket.contentHashCode()

        result = 31 * result + status.hashCode()

        result = 31 * result + attemptCount

        result = 31 * result + (lastError?.hashCode() ?: 0)

        result = 31 * result + createdAtEpochMilliseconds.hashCode()

        result = 31 * result + updatedAtEpochMilliseconds.hashCode()

        return result
    }
}
