package com.cbgm.sparrow.feature.identity.data.model

/** Persist the historical Room stage value for existing installations without a database migration. */
internal enum class IdentityExchangeStage(
    val persistedValue: String
) {
    OUTGOING_CHALLENGE_SENT("OUTGOING_CHALLENGE_SENT"),
    INCOMING_CHALLENGE_RECEIVED("INCOMING_CHALLENGE_RECEIVED"),
    ACCEPTANCE_SENT("ACCEPTANCE_SENT"),
    WAITING_FOR_READY("WAITING_FOR_READY"),
    MUTUAL_UNVERIFIED("MUTUAL_UNVERIFIED"),
    CLOSED("CLOSED"),
    EXCHANGE_INVALIDATED("AUTHORIZATION_REVOKED"),
    FAILED("FAILED")
}
