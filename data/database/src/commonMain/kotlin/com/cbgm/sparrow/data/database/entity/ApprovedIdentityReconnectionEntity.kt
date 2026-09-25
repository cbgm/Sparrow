package com.cbgm.sparrow.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

/** Durable, user-approved reconnection intent, not mutual authorization or successful delivery. */
@Entity(
    tableName = "approved_identity_reconnections",
    foreignKeys = [
        ForeignKey(
            entity = ContactEntity::class,
            parentColumns = ["id"],
            childColumns = ["peerId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class ApprovedIdentityReconnectionEntity(
    @PrimaryKey val peerId: String,
    val approvalId: String,
    val approvedAtEpochMilliseconds: Long,
    val originalInviteChallenge: ByteArray? = null,
    val originalInviteCreatedAtEpochMilliseconds: Long? = null,
    val originalInviteExpiresAtEpochMilliseconds: Long? = null,
    @ColumnInfo(defaultValue = "0") val originalInviteAutoSharesIdentity: Boolean = false,
    val originalInviterEncryptionPublicKey: ByteArray? = null,
    val originalInviterSigningPublicKey: ByteArray? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ApprovedIdentityReconnectionEntity

        if (approvedAtEpochMilliseconds != other.approvedAtEpochMilliseconds) return false
        if (originalInviteCreatedAtEpochMilliseconds != other.originalInviteCreatedAtEpochMilliseconds) return false
        if (originalInviteExpiresAtEpochMilliseconds != other.originalInviteExpiresAtEpochMilliseconds) return false
        if (originalInviteAutoSharesIdentity != other.originalInviteAutoSharesIdentity) return false
        if (peerId != other.peerId) return false
        if (approvalId != other.approvalId) return false
        if (!originalInviteChallenge.contentEquals(other.originalInviteChallenge)) return false
        if (!originalInviterEncryptionPublicKey.contentEquals(other.originalInviterEncryptionPublicKey)) return false
        if (!originalInviterSigningPublicKey.contentEquals(other.originalInviterSigningPublicKey)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = approvedAtEpochMilliseconds.hashCode()
        result = 31 * result + (originalInviteCreatedAtEpochMilliseconds?.hashCode() ?: 0)
        result = 31 * result + (originalInviteExpiresAtEpochMilliseconds?.hashCode() ?: 0)
        result = 31 * result + originalInviteAutoSharesIdentity.hashCode()
        result = 31 * result + peerId.hashCode()
        result = 31 * result + approvalId.hashCode()
        result = 31 * result + (originalInviteChallenge?.contentHashCode() ?: 0)
        result = 31 * result + (originalInviterEncryptionPublicKey?.contentHashCode() ?: 0)
        result = 31 * result + (originalInviterSigningPublicKey?.contentHashCode() ?: 0)
        return result
    }
}
