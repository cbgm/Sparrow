package com.cbgm.sparrow.feature.identity.domain.model

enum class DirectInvitationDirection {
    INCOMING,
    OUTGOING
}

data class DirectInvitationRecord(
    val invitationId: String,
    val contactId: String,
    val direction: DirectInvitationDirection,
    val createdAtEpochMilliseconds: Long,
    val expiresAtEpochMilliseconds: Long,
    val updatedAtEpochMilliseconds: Long,
    val peerDisplayName: String? = null,
    val peerSecondaryText: String? = null
) {
    init {
        require(invitationId.isNotBlank()) { "Invitation ID must not be blank" }
        require(contactId.isNotBlank()) { "Contact ID must not be blank" }
        require(createdAtEpochMilliseconds >= 0L) { "Invitation creation time must not be negative" }
        require(expiresAtEpochMilliseconds > createdAtEpochMilliseconds) {
            "Invitation expiration must be after creation"
        }
        require(updatedAtEpochMilliseconds >= createdAtEpochMilliseconds) {
            "Invitation update time must not precede creation"
        }
    }
}
