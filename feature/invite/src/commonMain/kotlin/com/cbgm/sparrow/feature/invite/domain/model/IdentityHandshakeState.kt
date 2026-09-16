package com.cbgm.sparrow.feature.invite.domain.model

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
