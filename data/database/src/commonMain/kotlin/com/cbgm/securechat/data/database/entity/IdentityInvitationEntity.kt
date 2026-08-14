package com.cbgm.securechat.data.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "identity_invitations",
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
        Index(value = ["direction"]),
        Index(value = ["state"])
    ]
)
data class IdentityInvitationEntity(
    @PrimaryKey
    val invitationId: String,
    val contactId: String,
    val direction: String,
    val state: String,
    val remoteDisplayName: String?,
    val inviteChallenge: ByteArray,
    val responseChallenge: ByteArray?,
    val remoteEncryptionPublicKey: ByteArray,
    val remoteSigningPublicKey: ByteArray,
    val createdAtEpochMilliseconds: Long,
    val expiresAtEpochMilliseconds: Long,
    val updatedAtEpochMilliseconds: Long,
    val lastError: String?,
    val localEncryptionPublicKey: ByteArray? = null,
    val localSigningPublicKey: ByteArray? = null
) {
    override fun equals(other: Any?): Boolean =
        other is IdentityInvitationEntity &&
            invitationId == other.invitationId &&
            contactId == other.contactId &&
            direction == other.direction &&
            state == other.state &&
            remoteDisplayName == other.remoteDisplayName &&
            inviteChallenge.contentEquals(other.inviteChallenge) &&
            responseChallenge.contentEqualsNullable(other.responseChallenge) &&
            remoteEncryptionPublicKey.contentEquals(other.remoteEncryptionPublicKey) &&
            remoteSigningPublicKey.contentEquals(other.remoteSigningPublicKey) &&
            createdAtEpochMilliseconds == other.createdAtEpochMilliseconds &&
            expiresAtEpochMilliseconds == other.expiresAtEpochMilliseconds &&
            updatedAtEpochMilliseconds == other.updatedAtEpochMilliseconds &&
            lastError == other.lastError &&
            localEncryptionPublicKey.contentEqualsNullable(other.localEncryptionPublicKey) &&
            localSigningPublicKey.contentEqualsNullable(other.localSigningPublicKey)

    override fun hashCode(): Int {
        var result = invitationId.hashCode()
        result = 31 * result + contactId.hashCode()
        result = 31 * result + direction.hashCode()
        result = 31 * result + state.hashCode()
        result = 31 * result + (remoteDisplayName?.hashCode() ?: 0)
        result = 31 * result + inviteChallenge.contentHashCode()
        result = 31 * result + (responseChallenge?.contentHashCode() ?: 0)
        result = 31 * result + remoteEncryptionPublicKey.contentHashCode()
        result = 31 * result + remoteSigningPublicKey.contentHashCode()
        result = 31 * result + createdAtEpochMilliseconds.hashCode()
        result = 31 * result + expiresAtEpochMilliseconds.hashCode()
        result = 31 * result + updatedAtEpochMilliseconds.hashCode()
        result = 31 * result + (lastError?.hashCode() ?: 0)
        result = 31 * result + (localEncryptionPublicKey?.contentHashCode() ?: 0)
        result = 31 * result + (localSigningPublicKey?.contentHashCode() ?: 0)
        return result
    }
}

private fun ByteArray?.contentEqualsNullable(other: ByteArray?): Boolean =
    when {
        this == null -> other == null
        other == null -> false
        else -> contentEquals(other)
    }
