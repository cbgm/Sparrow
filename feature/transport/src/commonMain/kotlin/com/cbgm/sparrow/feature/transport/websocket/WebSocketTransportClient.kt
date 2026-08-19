package com.cbgm.sparrow.feature.transport.websocket

import com.cbgm.sparrow.feature.transport.connection.TransportConnectionState
import com.cbgm.sparrow.feature.transport.gateway.model.FederatedEnvelope
import com.cbgm.sparrow.feature.transport.gateway.model.GatewayEnvelopeAcceptance
import com.cbgm.sparrow.feature.transport.gateway.model.GatewayTypingEvent
import com.cbgm.sparrow.feature.transport.gateway.model.TransportEnvelope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface WebSocketTransportClient {
    val connectionState: StateFlow<TransportConnectionState>
    val incomingEnvelopes: Flow<TransportEnvelope>
    val incomingTypingEvents: Flow<GatewayTypingEvent>

    fun connect(
        serverUrl: String,
        localRoutingId: String
    )

    suspend fun sendEnvelopeAndAwaitAcceptance(
        envelope: TransportEnvelope,
        timeoutMilliseconds: Long
    ): Result<Unit>

    suspend fun sendEnvelopeAndAwaitServerAcceptance(
        envelope: TransportEnvelope,
        timeoutMilliseconds: Long
    ): Result<GatewayEnvelopeAcceptance> =
        sendEnvelopeAndAwaitAcceptance(envelope, timeoutMilliseconds).map {
            GatewayEnvelopeAcceptance(
                envelopeId = envelope.envelopeId,
                expiresAtEpochMilliseconds = Long.MAX_VALUE
            )
        }

    suspend fun awaitRoutingAlias(
        routingAlias: String,
        timeoutMilliseconds: Long
    ): Result<Unit> = Result.success(Unit)

    suspend fun sendFederatedEnvelopeAndAwaitAcceptance(
        envelope: FederatedEnvelope,
        timeoutMilliseconds: Long
    ): Result<Unit> =
        Result.failure(UnsupportedOperationException("Federated envelopes are not supported"))

    suspend fun sendFederatedEnvelopeAndAwaitServerAcceptance(
        envelope: FederatedEnvelope,
        timeoutMilliseconds: Long
    ): Result<GatewayEnvelopeAcceptance> =
        sendFederatedEnvelopeAndAwaitAcceptance(envelope, timeoutMilliseconds).map {
            GatewayEnvelopeAcceptance(
                envelopeId = envelope.envelopeId,
                expiresAtEpochMilliseconds = envelope.expiresAtEpochMilliseconds
            )
        }

    suspend fun acknowledgeIncomingEnvelope(envelopeId: String): Result<Unit>

    suspend fun sendTypingState(
        recipientId: String,
        isTyping: Boolean
    ): Result<Unit>

    suspend fun disconnect()
}
