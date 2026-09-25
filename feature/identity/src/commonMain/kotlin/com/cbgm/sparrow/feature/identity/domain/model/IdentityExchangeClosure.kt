package com.cbgm.sparrow.feature.identity.domain.model

/** A snapshot of Identity-owned exchange state. Other modules decide the follow-up action. */
enum class IdentityExchangeClosurePhase {
    INCOMING_PENDING,
    ACTIVE,
    TERMINAL,
    ALREADY_CLOSED
}

class IdentityExchangeClosure(
    val exchangeId: String,
    val peerId: String,
    val phase: IdentityExchangeClosurePhase,
    inviteChallenge: ByteArray
) {
    val inviteChallenge: ByteArray = inviteChallenge.copyOf()
}
