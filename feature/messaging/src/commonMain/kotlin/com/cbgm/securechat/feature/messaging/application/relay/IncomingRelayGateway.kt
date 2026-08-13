package com.cbgm.securechat.feature.messaging.application.relay

import kotlinx.coroutines.flow.Flow

data class IncomingRelayEnvelope(
    val envelopeId: String,
    val senderRelayId: String,
    val encodedTransportPayload: String
)

interface IncomingRelayGateway {
    val incomingEnvelopes: Flow<IncomingRelayEnvelope>

    suspend fun acknowledge(envelopeId: String): Result<Unit>
}
