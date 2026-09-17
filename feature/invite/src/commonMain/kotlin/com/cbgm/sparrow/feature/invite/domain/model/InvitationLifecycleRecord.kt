package com.cbgm.sparrow.feature.invite.domain.model

data class InvitationLifecycleRecord(
    val invitationId: String,
    val payloadType: InvitationPayloadType,
    val payloadId: String,
    val peerId: String,
    val direction: InvitationDirection,
    val createdAtEpochMilliseconds: Long,
    val expiresAtEpochMilliseconds: Long,
    val updatedAtEpochMilliseconds: Long
) {
    init {
        require(invitationId.isNotBlank()) { "Invitation ID must not be blank" }
        require(payloadId.isNotBlank()) { "Invitation payload ID must not be blank" }
        require(peerId.isNotBlank()) { "Invitation peer ID must not be blank" }
        require(createdAtEpochMilliseconds >= 0L) { "Invitation creation time must not be negative" }
        require(expiresAtEpochMilliseconds > createdAtEpochMilliseconds) {
            "Invitation expiration must be after creation"
        }
        require(updatedAtEpochMilliseconds >= createdAtEpochMilliseconds) {
            "Invitation update time must not precede creation"
        }
    }
}
