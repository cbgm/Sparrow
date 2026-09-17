package com.cbgm.sparrow.feature.identity.domain.model

enum class IdentityHandshakeState {
    ACCEPTANCE_SENT,
    WAITING_FOR_READY,
    MUTUAL_UNVERIFIED,
    AUTHORIZATION_REVOKED,
    FAILED
}
