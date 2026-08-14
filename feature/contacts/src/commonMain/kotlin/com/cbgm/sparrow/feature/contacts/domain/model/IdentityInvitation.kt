package com.cbgm.sparrow.feature.contacts.domain.model

enum class IdentityInvitationDirection {
    INCOMING,
    OUTGOING
}

enum class IdentityHandshakeState {
    INVITE_SENT,
    AWAITING_ACCEPTANCE,
    ACCEPTANCE_SENT,
    WAITING_FOR_READY,
    MUTUAL_UNVERIFIED,
    DECLINED,
    CONVERSATION_DELETED,
    EXPIRED,
    FAILED
}

data class PendingContactInvitation(
    val invitationId: String,
    val contactId: String,
    val contactName: String?,
    val contactPhoneNumber: String?,
    val expiresAtEpochMilliseconds: Long
)
