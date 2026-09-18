package com.cbgm.sparrow.data.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "invitations",
    indices = [
        Index(value = ["payloadType", "payloadId"]),
        Index(value = ["peerId"]),
        Index(value = ["direction"]),
        Index(value = ["status"]),
        Index(value = ["payloadType", "payloadId", "peerId", "direction"])
    ]
)
data class InvitationEntity(
    @PrimaryKey
    val invitationId: String,
    val payloadType: String,
    val payloadId: String,
    val peerId: String,
    val peerDisplayName: String? = null,
    val peerSecondaryText: String? = null,
    val direction: String,
    val status: String,
    val createdAtEpochMilliseconds: Long,
    val expiresAtEpochMilliseconds: Long,
    val updatedAtEpochMilliseconds: Long,
    val viewedAtEpochMilliseconds: Long? = null,
    val hiddenAtEpochMilliseconds: Long? = null,
    val resultAction: String? = null
) {
    init {
        require(invitationId.isNotBlank()) { "Invitation ID must not be blank" }
        require(payloadType.isNotBlank()) { "Invitation payload type must not be blank" }
        require(payloadId.isNotBlank()) { "Invitation payload ID must not be blank" }
        require(peerId.isNotBlank()) { "Invitation peer ID must not be blank" }
        require(direction.isNotBlank()) { "Invitation direction must not be blank" }
        require(status.isNotBlank()) { "Invitation status must not be blank" }
        require(createdAtEpochMilliseconds >= 0L) { "Invitation timestamp must not be negative" }
        require(expiresAtEpochMilliseconds > createdAtEpochMilliseconds) {
            "Invitation expiration must be after its creation"
        }
        require(updatedAtEpochMilliseconds >= createdAtEpochMilliseconds) {
            "Invitation update timestamp must not precede creation"
        }
    }
}
