package com.cbgm.sparrow.data.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "group_member_keys",
    primaryKeys = ["groupId", "epoch", "contactId"],
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
        Index(value = ["contactId"])
    ]
)
data class GroupMemberKeyEntity(
    val groupId: String,
    val epoch: Int,
    val contactId: String,
    val encryptionPublicKey: ByteArray,
    val signingPublicKey: ByteArray,
    val role: String
) {
    init {
        require(groupId.isNotBlank()) { "Group ID must not be blank" }
        require(epoch > 0) { "Group epoch must be positive" }
        require(contactId.isNotBlank()) { "Contact ID must not be blank" }
        require(encryptionPublicKey.isNotEmpty()) { "Member encryption public key must not be empty" }
        require(signingPublicKey.isNotEmpty()) { "Member signing public key must not be empty" }
        require(role.isNotBlank()) { "Group member role must not be blank" }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GroupMemberKeyEntity) return false

        return groupId == other.groupId &&
            epoch == other.epoch &&
            contactId == other.contactId &&
            encryptionPublicKey.contentEquals(other.encryptionPublicKey) &&
            signingPublicKey.contentEquals(other.signingPublicKey) &&
            role == other.role
    }

    override fun hashCode(): Int {
        var result = groupId.hashCode()
        result = 31 * result + epoch
        result = 31 * result + contactId.hashCode()
        result = 31 * result + encryptionPublicKey.contentHashCode()
        result = 31 * result + signingPublicKey.contentHashCode()
        result = 31 * result + role.hashCode()
        return result
    }
}
