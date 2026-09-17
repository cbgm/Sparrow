package com.cbgm.sparrow.data.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "identity_exchanges",
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
        Index(value = ["stage"])
    ]
)
data class IdentityExchangeEntity(
    @PrimaryKey
    val exchangeId: String,
    val contactId: String,
    val direction: String,
    val stage: String,
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
        other is IdentityExchangeEntity &&
            exchangeId == other.exchangeId &&
            contactId == other.contactId &&
            direction == other.direction &&
            stage == other.stage &&
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
        var result = exchangeId.hashCode()
        result = 31 * result + contactId.hashCode()
        result = 31 * result + direction.hashCode()
        result = 31 * result + stage.hashCode()
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
