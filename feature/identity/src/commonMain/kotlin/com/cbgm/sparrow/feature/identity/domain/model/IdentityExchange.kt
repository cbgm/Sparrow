package com.cbgm.sparrow.feature.identity.domain.model

enum class IdentityExchangeDirection {
    INCOMING,
    OUTGOING
}

data class IdentityExchange(
    val exchangeId: String,
    val peerId: String,
    val direction: IdentityExchangeDirection,
    val createdAtEpochMilliseconds: Long,
    val expiresAtEpochMilliseconds: Long,
    val updatedAtEpochMilliseconds: Long
) {
    init {
        require(exchangeId.isNotBlank()) { "Exchange ID must not be blank" }
        require(peerId.isNotBlank()) { "Peer ID must not be blank" }
        require(createdAtEpochMilliseconds >= 0L) { "Exchange creation time must not be negative" }
        require(expiresAtEpochMilliseconds > createdAtEpochMilliseconds) {
            "Exchange expiration must be after creation"
        }
        require(updatedAtEpochMilliseconds >= createdAtEpochMilliseconds) {
            "Exchange update time must not precede creation"
        }
    }
}
