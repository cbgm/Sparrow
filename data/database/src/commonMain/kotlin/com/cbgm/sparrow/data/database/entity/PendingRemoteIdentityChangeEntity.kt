package com.cbgm.sparrow.data.database.entity

import androidx.room.ColumnInfo
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
    val expiresAtEpochMilliseconds: Long,
    /** The user approved the shown new keys. Does not prove continuity with the old identity. */
    val fingerprintConfirmedAtEpochMilliseconds: Long? = null,
    /** Snapshots bind a later replacement decision to the exact old keys present at confirmation. */
    val confirmedPreviousEncryptionPublicKey: ByteArray? = null,
    val confirmedPreviousSigningPublicKey: ByteArray? = null,
    /** Captured ONLY from a signature-verified incoming ContactInvitePacket. Null on
     * an unexpected changed-key ACCEPTANCE, which uses the older reverse-invite fallback. */
    val originalInviteChallenge: ByteArray? = null,
    val originalInviteCreatedAtEpochMilliseconds: Long? = null,
    @ColumnInfo(defaultValue = "0") val originalInviteAutoSharesIdentity: Boolean = false
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PendingRemoteIdentityChangeEntity

        if (receivedAtEpochMilliseconds != other.receivedAtEpochMilliseconds) return false
        if (expiresAtEpochMilliseconds != other.expiresAtEpochMilliseconds) return false
        if (fingerprintConfirmedAtEpochMilliseconds != other.fingerprintConfirmedAtEpochMilliseconds) return false
        if (peerId != other.peerId) return false
        if (sourcePeerId != other.sourcePeerId) return false
        if (invitationId != other.invitationId) return false
        if (!proposedEncryptionPublicKey.contentEquals(other.proposedEncryptionPublicKey)) return false
        if (!proposedSigningPublicKey.contentEquals(other.proposedSigningPublicKey)) return false
        if (!confirmedPreviousEncryptionPublicKey.contentEquals(other.confirmedPreviousEncryptionPublicKey)) return false
        if (!confirmedPreviousSigningPublicKey.contentEquals(other.confirmedPreviousSigningPublicKey)) return false
        if (!originalInviteChallenge.contentEquals(other.originalInviteChallenge)) return false
        if (originalInviteCreatedAtEpochMilliseconds != other.originalInviteCreatedAtEpochMilliseconds) return false
        if (originalInviteAutoSharesIdentity != other.originalInviteAutoSharesIdentity) return false

        return true
    }

    override fun hashCode(): Int {
        var result = receivedAtEpochMilliseconds.hashCode()
        result = 31 * result + expiresAtEpochMilliseconds.hashCode()
        result = 31 * result + (fingerprintConfirmedAtEpochMilliseconds?.hashCode() ?: 0)
        result = 31 * result + peerId.hashCode()
        result = 31 * result + sourcePeerId.hashCode()
        result = 31 * result + invitationId.hashCode()
        result = 31 * result + proposedEncryptionPublicKey.contentHashCode()
        result = 31 * result + proposedSigningPublicKey.contentHashCode()
        result = 31 * result + (confirmedPreviousEncryptionPublicKey?.contentHashCode() ?: 0)
        result = 31 * result + (confirmedPreviousSigningPublicKey?.contentHashCode() ?: 0)
        result = 31 * result + (originalInviteChallenge?.contentHashCode() ?: 0)
        result = 31 * result + (originalInviteCreatedAtEpochMilliseconds?.hashCode() ?: 0)
        result = 31 * result + originalInviteAutoSharesIdentity.hashCode()
        return result
    }
}
