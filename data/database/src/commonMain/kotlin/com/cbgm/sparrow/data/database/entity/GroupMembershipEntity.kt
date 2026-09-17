package com.cbgm.sparrow.data.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "group_memberships",
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
        Index(value = ["sourceInvitationId"], unique = true),
        Index(value = ["groupId"]),
        Index(value = ["contactId"]),
        Index(value = ["groupId", "contactId"], unique = true)
    ]
)
data class GroupMembershipEntity(
    @PrimaryKey
    val membershipId: String,
    val sourceInvitationId: String,
    val groupId: String,
    val contactId: String,
    val perspective: String,
    val status: String,
    val challenge: ByteArray,
    val ownerEncryptionPublicKey: ByteArray? = null,
    val ownerSigningPublicKey: ByteArray? = null,
    val createdAtEpochMilliseconds: Long,
    val updatedAtEpochMilliseconds: Long
) {
    init {
        require(membershipId.isNotBlank()) { "Membership ID must not be blank" }
        require(sourceInvitationId.isNotBlank()) { "Source invitation ID must not be blank" }
        require(groupId.isNotBlank()) { "Group ID must not be blank" }
        require(contactId.isNotBlank()) { "Contact ID must not be blank" }
        require(perspective.isNotBlank()) { "Membership perspective must not be blank" }
        require(status.isNotBlank()) { "Membership status must not be blank" }
        require(challenge.isNotEmpty()) { "Membership challenge must not be empty" }
        require(createdAtEpochMilliseconds >= 0L) { "Membership timestamp must not be negative" }
        require(updatedAtEpochMilliseconds >= createdAtEpochMilliseconds) {
            "Membership update timestamp must not precede creation"
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GroupMembershipEntity) return false

        return membershipId == other.membershipId &&
            sourceInvitationId == other.sourceInvitationId &&
            groupId == other.groupId &&
            contactId == other.contactId &&
            perspective == other.perspective &&
            status == other.status &&
            challenge.contentEquals(other.challenge) &&
            ownerEncryptionPublicKey.contentEqualsNullable(other.ownerEncryptionPublicKey) &&
            ownerSigningPublicKey.contentEqualsNullable(other.ownerSigningPublicKey) &&
            createdAtEpochMilliseconds == other.createdAtEpochMilliseconds &&
            updatedAtEpochMilliseconds == other.updatedAtEpochMilliseconds
    }

    override fun hashCode(): Int {
        var result = membershipId.hashCode()
        result = 31 * result + sourceInvitationId.hashCode()
        result = 31 * result + groupId.hashCode()
        result = 31 * result + contactId.hashCode()
        result = 31 * result + perspective.hashCode()
        result = 31 * result + status.hashCode()
        result = 31 * result + challenge.contentHashCode()
        result = 31 * result + (ownerEncryptionPublicKey?.contentHashCode() ?: 0)
        result = 31 * result + (ownerSigningPublicKey?.contentHashCode() ?: 0)
        result = 31 * result + createdAtEpochMilliseconds.hashCode()
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
