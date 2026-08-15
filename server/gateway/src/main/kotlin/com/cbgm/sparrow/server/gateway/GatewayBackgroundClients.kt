package com.cbgm.sparrow.server.gateway

import com.cbgm.sparrow.server.protocol.ClientRouteRegistration
import com.cbgm.sparrow.server.protocol.GatewayServerMessage
import com.cbgm.sparrow.server.protocol.TransportEnvelope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

internal class BestEffortPresenceClient(
    private val delegate: PresenceClient
) : PresenceClient {
    private val scope = CoroutineScope(SupervisorJob())
    private val slots = Semaphore(MAX_CONCURRENT_PRESENCE_SYNCS)

    override suspend fun register(registration: ClientRouteRegistration): Boolean {
        scope.launch {
            slots.withPermit {
                runCatching { delegate.register(registration) }
            }
        }
        return true
    }

    override suspend fun remove(
        routingId: String,
        connectionId: String
    ) {
        scope.launch {
            slots.withPermit {
                runCatching { delegate.remove(routingId, connectionId) }
            }
        }
    }

    fun close() {
        scope.cancel()
    }

    private companion object {
        const val MAX_CONCURRENT_PRESENCE_SYNCS = 16
    }
}

internal class GatewayPushDispatcher(
    private val pushClient: LegacyPushClient,
    private val markFederationStored: suspend (String) -> Unit
) {
    private val scope = CoroutineScope(SupervisorJob())
    private val slots = Semaphore(MAX_CONCURRENT_PUSH_OPERATIONS)

    fun scheduleFallback(
        envelope: TransportEnvelope,
        federationEnvelopeId: String
    ) {
        launchPushOperation {
            val stored = pushClient.store(envelope)
            if (stored) {
                markFederationStored(federationEnvelopeId)
            }
        }
    }

    fun deliverPending(connection: GatewayConnection) {
        launchPushOperation {
            pushClient
                .pendingForRoutingIds(connection.routingIds())
                .forEach { envelope ->
                    connection.send(
                        GatewayServerMessage.IncomingEnvelope(
                            envelope = envelope
                        )
                    )
                }
        }
    }

    fun acknowledge(
        connection: GatewayConnection,
        envelopeId: String
    ) {
        launchPushOperation {
            pushClient.acknowledgeForRoutingIds(
                routingIds = connection.routingIds(),
                envelopeId = envelopeId
            )
        }
    }

    fun close() {
        scope.cancel()
    }

    private fun launchPushOperation(operation: suspend () -> Unit) {
        scope.launch {
            slots.withPermit {
                runCatching {
                    operation()
                }
            }
        }
    }

    private companion object {
        const val MAX_CONCURRENT_PUSH_OPERATIONS = 8
    }
}

internal suspend fun LegacyPushClient.pendingForRoutingIds(
    routingIds: Set<String>
): List<TransportEnvelope> =
    routingIds
        .flatMap { routingId -> pending(recipientId = routingId) }
        .distinctBy(TransportEnvelope::envelopeId)

internal suspend fun LegacyPushClient.acknowledgeForRoutingIds(
    routingIds: Set<String>,
    envelopeId: String
) {
    routingIds.forEach { routingId ->
        acknowledge(
            recipientId = routingId,
            envelopeId = envelopeId
        )
    }
}
