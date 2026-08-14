package com.cbgm.sparrow.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "group_invitations",
    foreignKeys = [
        ForeignKey(
            entity = ConversationEntity::class,
            parentColumns = ["id"],
            childColumns = ["groupId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ContactEntity::class,
            parentColumns = ["id"],
            childColumns = ["contactId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["groupId"]),
        Index(value = ["contactId"]),
        Index(value = ["groupId", "contactId"], unique = true)
    ]
)
data class GroupInvitationEntity(
    @PrimaryKey
    val invitationId: String,
    val groupId: String,
    val contactId: String,
    @ColumnInfo(defaultValue = "'INCOMING'")
    val direction: String,
    val status: String,
    val challenge: ByteArray,
    val ownerEncryptionPublicKey: ByteArray? = null,
    val ownerSigningPublicKey: ByteArray? = null,
    val createdAtEpochMilliseconds: Long,
    val expiresAtEpochMilliseconds: Long,
    val updatedAtEpochMilliseconds: Long
) {
    init {
        require(invitationId.isNotBlank()) { "Invitation ID must not be blank" }
        require(groupId.isNotBlank()) { "Group ID must not be blank" }
        require(contactId.isNotBlank()) { "Contact ID must not be blank" }
        require(direction.isNotBlank()) { "Invitation direction must not be blank" }
        require(status.isNotBlank()) { "Invitation status must not be blank" }
        require(challenge.isNotEmpty()) { "Invitation challenge must not be empty" }
        require(createdAtEpochMilliseconds >= 0L) { "Invitation timestamp must not be negative" }
        require(expiresAtEpochMilliseconds > createdAtEpochMilliseconds) {
            "Invitation expiration must be after its creation"
        }
        require(updatedAtEpochMilliseconds >= createdAtEpochMilliseconds) {
            "Invitation update timestamp must not precede creation"
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GroupInvitationEntity) return false

        return invitationId == other.invitationId &&
            groupId == other.groupId &&
            contactId == other.contactId &&
            direction == other.direction &&
            status == other.status &&
            challenge.contentEquals(other.challenge) &&
            ownerEncryptionPublicKey.contentEqualsNullable(other.ownerEncryptionPublicKey) &&
            ownerSigningPublicKey.contentEqualsNullable(other.ownerSigningPublicKey) &&
            createdAtEpochMilliseconds == other.createdAtEpochMilliseconds &&
            expiresAtEpochMilliseconds == other.expiresAtEpochMilliseconds &&
            updatedAtEpochMilliseconds == other.updatedAtEpochMilliseconds
    }

    override fun hashCode(): Int {
        var result = invitationId.hashCode()
        result = 31 * result + groupId.hashCode()
        result = 31 * result + contactId.hashCode()
        result = 31 * result + direction.hashCode()
        result = 31 * result + status.hashCode()
        result = 31 * result + challenge.contentHashCode()
        result = 31 * result + (ownerEncryptionPublicKey?.contentHashCode() ?: 0)
        result = 31 * result + (ownerSigningPublicKey?.contentHashCode() ?: 0)
        result = 31 * result + createdAtEpochMilliseconds.hashCode()
        result = 31 * result + expiresAtEpochMilliseconds.hashCode()
        result = 31 * result + updatedAtEpochMilliseconds.hashCode()
        return result
    }
}

private fun ByteArray?.contentEqualsNullable(other: ByteArray?): Boolean =
    when {
        this == null -> other == null
        other == null -> false
        else -> contentEquals(other)
    }
