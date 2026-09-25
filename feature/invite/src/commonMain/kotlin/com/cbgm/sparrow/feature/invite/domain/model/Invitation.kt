package com.cbgm.sparrow.feature.invite.domain.model

enum class InvitationPayloadType {
    DIRECT,
    GROUP
}

enum class InvitationDirection {
    INCOMING,
    OUTGOING
}

enum class InvitationStatus {
    PENDING,
    DECLINED,
    EXPIRED,
    FAILED
}

data class Invitation(
    val invitationId: String,
    val payloadType: InvitationPayloadType,
    val payloadId: String,
    val peerId: String,
    val peerDisplayName: String?,
    val peerSecondaryText: String?,
    val direction: InvitationDirection,
    val status: InvitationStatus,
    val expiresAtEpochMilliseconds: Long,
    val updatedAtEpochMilliseconds: Long,
    val hasUnreadUpdate: Boolean
)

data class InvitationsContext(
    val incoming: List<Invitation> = emptyList(),
    val outgoing: List<Invitation> = emptyList()
)
