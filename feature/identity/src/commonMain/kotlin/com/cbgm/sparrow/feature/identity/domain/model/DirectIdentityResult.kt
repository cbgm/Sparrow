package com.cbgm.sparrow.feature.identity.domain.model

enum class DirectIdentityResultType {
    INCOMING_INVITATION,
    AUTHORIZED,
    REMOTE_DECLINED,
    AUTHORIZATION_REVOKED,
    FAILED
}

data class DirectIdentityResult(
    val invitationId: String,
    val contactId: String,
    val direction: DirectInvitationDirection,
    val type: DirectIdentityResultType,
    val createdAtEpochMilliseconds: Long,
    val expiresAtEpochMilliseconds: Long,
    val updatedAtEpochMilliseconds: Long,
    val peerDisplayName: String? = null,
    val wasKnownPeerAtReceive: Boolean? = null
)
