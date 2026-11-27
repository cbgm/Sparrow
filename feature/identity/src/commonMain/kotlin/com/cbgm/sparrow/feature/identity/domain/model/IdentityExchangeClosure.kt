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

    /** [exchangeId] is still this peer's exchange and was neither finished nor closed. */
    fun isOpenFor(exchangeId: String): Boolean =
        this.exchangeId == exchangeId &&
            phase != IdentityExchangeClosurePhase.TERMINAL &&
            phase != IdentityExchangeClosurePhase.ALREADY_CLOSED

    /** [exchangeId] is still this peer's exchange and is active, not only pending. */
    fun isActiveFor(exchangeId: String): Boolean =
        this.exchangeId == exchangeId && phase == IdentityExchangeClosurePhase.ACTIVE
}
