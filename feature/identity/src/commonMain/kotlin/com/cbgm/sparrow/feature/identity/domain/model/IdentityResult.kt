package com.cbgm.sparrow.feature.identity.domain.model

enum class IdentityResultStatus {
    INCOMING_EXCHANGE,
    ESTABLISHED,
    REMOTE_DECLINED,
    EXCHANGE_INVALIDATED,
    FAILED
}

data class IdentityResult(
    val exchangeId: String,
    val peerId: String,
    val direction: IdentityExchangeDirection,
    val status: IdentityResultStatus,
    val createdAtEpochMilliseconds: Long,
    val expiresAtEpochMilliseconds: Long,
    val updatedAtEpochMilliseconds: Long,
    val wasKnownPeerAtReceive: Boolean? = null
)
