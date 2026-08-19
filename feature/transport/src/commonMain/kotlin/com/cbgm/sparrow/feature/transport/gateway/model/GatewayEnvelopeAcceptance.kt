package com.cbgm.sparrow.feature.transport.gateway.model

data class GatewayEnvelopeAcceptance(
    val envelopeId: String,
    val expiresAtEpochMilliseconds: Long
) {
    init {
        require(envelopeId.isNotBlank()) { "Envelope ID must not be blank" }
        require(expiresAtEpochMilliseconds > 0L) { "Envelope expiry must be positive" }
    }
}
