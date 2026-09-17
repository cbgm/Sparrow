package com.cbgm.sparrow.feature.conversationorchestration.data.direct.identity

internal enum class DirectIdentityExchangeStage {
    OUTGOING_CHALLENGE_SENT,
    INCOMING_CHALLENGE_RECEIVED,
    ACCEPTANCE_SENT,
    WAITING_FOR_READY,
    MUTUAL_UNVERIFIED,
    CLOSED,
    AUTHORIZATION_REVOKED,
    FAILED
}
