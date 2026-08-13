package com.cbgm.securechat.feature.messaging.application.routing

import kotlinx.coroutines.flow.Flow

data class IncomingTransportEnvelope(
    val envelopeId: String,
    val senderRoutingId: String,
    val encodedTransportPayload: String
)

interface IncomingEnvelopeGateway {
    val incomingEnvelopes: Flow<IncomingTransportEnvelope>

    suspend fun acknowledge(envelopeId: String): Result<Unit>
}
