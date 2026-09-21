package com.cbgm.sparrow.data.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

/** A signed *candidate* identity. No old contact, trust or conversation is modified. */
@Entity(
    tableName = "pending_remote_identity_changes",
    foreignKeys = [
        ForeignKey(
            entity = ContactEntity::class,
            parentColumns = ["id"],
            childColumns = ["peerId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class PendingRemoteIdentityChangeEntity(
    @PrimaryKey val peerId: String,
    val sourcePeerId: String,
    val invitationId: String,
    val proposedEncryptionPublicKey: ByteArray,
    val proposedSigningPublicKey: ByteArray,
    val receivedAtEpochMilliseconds: Long,
    val expiresAtEpochMilliseconds: Long
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PendingRemoteIdentityChangeEntity

        if (receivedAtEpochMilliseconds != other.receivedAtEpochMilliseconds) return false
        if (expiresAtEpochMilliseconds != other.expiresAtEpochMilliseconds) return false
        if (peerId != other.peerId) return false
        if (sourcePeerId != other.sourcePeerId) return false
        if (invitationId != other.invitationId) return false
        if (!proposedEncryptionPublicKey.contentEquals(other.proposedEncryptionPublicKey)) return false
        if (!proposedSigningPublicKey.contentEquals(other.proposedSigningPublicKey)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = receivedAtEpochMilliseconds.hashCode()
        result = 31 * result + expiresAtEpochMilliseconds.hashCode()
        result = 31 * result + peerId.hashCode()
        result = 31 * result + sourcePeerId.hashCode()
        result = 31 * result + invitationId.hashCode()
        result = 31 * result + proposedEncryptionPublicKey.contentHashCode()
        result = 31 * result + proposedSigningPublicKey.contentHashCode()
        return result
    }
}
