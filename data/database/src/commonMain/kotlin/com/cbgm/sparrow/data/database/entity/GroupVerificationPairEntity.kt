package com.cbgm.sparrow.data.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "group_verification_pairs",
    primaryKeys = ["groupId", "invitationId"],
    foreignKeys = [
        ForeignKey(
            entity = ConversationEntity::class,
            parentColumns = ["id"],
            childColumns = ["groupId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["groupId"]),
        Index(value = ["contactId"])
    ]
)
data class GroupVerificationPairEntity(
    val groupId: String,
    val invitationId: String,
    val contactId: String?,
    val displayName: String,
    val membershipStatus: String,
    val participantEncryptionPublicKey: ByteArray?,
    val participantSigningPublicKey: ByteArray?,
    val adminVerifiedParticipant: Boolean,
    val participantVerifiedAdmin: Boolean,
    val updatedAtEpochMilliseconds: Long
) {
    init {
        require(groupId.isNotBlank()) { "Group ID must not be blank" }
        require(invitationId.isNotBlank()) { "Invitation ID must not be blank" }
        require(contactId == null || contactId.isNotBlank()) { "Contact ID must not be blank" }
        require(displayName.isNotBlank()) { "Display name must not be blank" }
        require(membershipStatus == ACTIVE_STATUS || membershipStatus == PENDING_STATUS) {
            "Unsupported group verification membership status"
        }
        require(updatedAtEpochMilliseconds >= 0L) { "Update timestamp must not be negative" }
    }

    companion object {
        const val ACTIVE_STATUS = "ACTIVE"
        const val PENDING_STATUS = "PENDING"
    }
}
