package com.cbgm.securechat.feature.transport.websocket

import com.cbgm.securechat.feature.transport.connection.TransportConnectionState
import com.cbgm.securechat.feature.transport.relay.model.FederatedEnvelope
import com.cbgm.securechat.feature.transport.relay.model.RelayEnvelope
import com.cbgm.securechat.feature.transport.relay.model.RelayTypingEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface WebSocketTransportClient {
    val connectionState: StateFlow<TransportConnectionState>
    val incomingEnvelopes: Flow<RelayEnvelope>
    val incomingTypingEvents: Flow<RelayTypingEvent>

    fun connect(
        serverUrl: String,
        localRelayId: String
    )

    suspend fun sendEnvelopeAndAwaitAcceptance(
        envelope: RelayEnvelope,
        timeoutMilliseconds: Long
    ): Result<Unit>

    suspend fun awaitRoutingAlias(
        routingAlias: String,
        timeoutMilliseconds: Long
    ): Result<Unit> = Result.success(Unit)

    suspend fun sendFederatedEnvelopeAndAwaitAcceptance(
        envelope: FederatedEnvelope,
        timeoutMilliseconds: Long
    ): Result<Unit> = Result.failure(UnsupportedOperationException("Federated envelopes are not supported"))

    suspend fun acknowledgeIncomingEnvelope(envelopeId: String): Result<Unit>

    suspend fun sendTypingState(
        recipientId: String,
        isTyping: Boolean
    ): Result<Unit>

    suspend fun disconnect()
}
