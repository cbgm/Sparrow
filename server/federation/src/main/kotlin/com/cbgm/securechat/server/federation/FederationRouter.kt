package com.cbgm.securechat.server.federation

import com.cbgm.securechat.server.protocol.EnvelopeAcceptanceState
import com.cbgm.securechat.server.protocol.FederatedEnvelope
import com.cbgm.securechat.server.protocol.FederatedTypingEvent
import com.cbgm.securechat.server.protocol.FederationAcknowledgement
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

private const val DEFAULT_RETRY_BASE_DELAY_MILLISECONDS = 5_000L
private const val DEFAULT_RETRY_MAXIMUM_DELAY_MILLISECONDS = 5L * 60L * 1_000L

class FederationRouter(
    private val localNodeId: String,
    private val presenceDirectory: PresenceDirectoryClient,
    private val nodeRegistry: NodeRegistryClient,
    private val localGateway: LocalGatewayClient,
    private val remoteFederation: RemoteFederationClient,
    private val mailbox: MailboxClient,
    private val localTypingGateway: LocalTypingGatewayClient = LocalTypingGatewayClient { false },
    private val remoteTypingFederation: RemoteTypingFederationClient =
        RemoteTypingFederationClient { _, _ -> false },
    private val queue: OutboundEnvelopeStorage = OutboundEnvelopeQueue(),
    private val retryBaseDelayMilliseconds: Long = DEFAULT_RETRY_BASE_DELAY_MILLISECONDS,
    private val retryMaximumDelayMilliseconds: Long = DEFAULT_RETRY_MAXIMUM_DELAY_MILLISECONDS,
    private val now: () -> Long = System::currentTimeMillis
) {
    private val deliveryMutex = Mutex()
    private val localRouteResolver =
        localGateway as? LocalRouteResolver ?: LocalRouteResolver { null }
    private val peerRouter =
        FederationPeerRouter(
            localNodeId = localNodeId,
            peerNodeDirectory =
                nodeRegistry as? PeerNodeDirectory ?: PeerNodeDirectory { emptyList() },
            remoteRouteResolver =
                remoteFederation as? RemoteRouteResolver ?: RemoteRouteResolver { _, _ -> null },
            remoteFederation = remoteFederation,
            remoteTypingFederation = remoteTypingFederation
        )

    init {
        require(retryBaseDelayMilliseconds > 0L)
        require(retryMaximumDelayMilliseconds >= retryBaseDelayMilliseconds)
    }

    suspend fun route(envelope: FederatedEnvelope): FederationAcknowledgement =
        deliveryMutex.withLock {
            val existing = queue.get(envelope.envelopeId)
            if (existing?.state == EnvelopeAcceptanceState.STORED_AT_DESTINATION) {
                return@withLock FederationAcknowledgement(
                    envelope.envelopeId,
                    existing.state,
                    duplicate = true
                )
            }

            val queued = queue.enqueue(envelope)
            deliver(queued)
        }

    suspend fun retryPending(limit: Int): Int {
        require(limit > 0)
        val due = queue.pendingDue(now(), limit)
        var processed = 0
        due.forEach { candidate ->
            deliveryMutex.withLock {
                val current = queue.get(candidate.envelope.envelopeId)
                if (
                    current?.state == EnvelopeAcceptanceState.QUEUED_AT_GATEWAY &&
                    current.nextAttemptAtEpochMilliseconds <= now()
                ) {
                    deliver(current)
                    processed += 1
                }
            }
        }
        return processed
    }

    suspend fun pendingCount(): Int = queue.pendingCount()

    suspend fun markStored(envelopeId: String) {
        queue.markStored(envelopeId)
    }

    suspend fun routeTyping(event: FederatedTypingEvent): Boolean {
        val localRoutingId =
            runCatching {
                localRouteResolver.resolve(event.recipientRoutingId)
            }.getOrNull()
        if (localRoutingId != null) {
            return localTypingGateway.deliver(
                event.copy(recipientRoutingId = localRoutingId)
            )
        }

        if (peerRouter.routeTyping(event)) {
            return true
        }

        val routes =
            runCatching {
                presenceDirectory.resolve(event.recipientRoutingId).routes
            }.getOrDefault(emptyList())
        for (route in routes.sortedByDescending { it.generation }) {
            val delivered =
                runCatching {
                    val routedEvent = event.copy(recipientRoutingId = route.routingId)
                    if (route.nodeId == localNodeId) {
                        localTypingGateway.deliver(routedEvent)
                    } else {
                        val descriptor = nodeRegistry.find(route.nodeId) ?: return@runCatching false
                        remoteTypingFederation.deliver(descriptor, routedEvent)
                    }
                }.getOrDefault(false)

            if (delivered) {
                return true
            }
        }
        return false
    }

    private suspend fun deliver(entry: OutboundEnvelopeEntry): FederationAcknowledgement {
        val nextAttemptAt = now() + retryDelay(entry.attempts)
        val attempted = queue.markAttempt(entry.envelope.envelopeId, nextAttemptAt)
        val onlineAcknowledgement = attempted?.let { routeOnline(it.envelope) }
        val acknowledgement =
            onlineAcknowledgement
                ?: attempted
                    ?.let { candidate -> bindBootstrapRecipient(candidate) }
                    ?.let { candidate -> storeInMailbox(candidate.envelope) }
        val storedAcknowledgement =
            acknowledgement?.takeIf {
                it.state == EnvelopeAcceptanceState.STORED_AT_DESTINATION
            }

        if (storedAcknowledgement != null) {
            queue.markStored(entry.envelope.envelopeId)
        }

        return storedAcknowledgement
            ?: FederationAcknowledgement(
                entry.envelope.envelopeId,
                EnvelopeAcceptanceState.QUEUED_AT_GATEWAY
            )
    }

    private suspend fun bindBootstrapRecipient(entry: OutboundEnvelopeEntry): OutboundEnvelopeEntry {
        val recipientRoutingId = entry.envelope.recipientDeviceRoutingId
        if (!recipientRoutingId.startsWith(BOOTSTRAP_ROUTING_ID_PREFIX)) {
            return entry
        }

        val canonicalRoute =
            runCatching {
                presenceDirectory
                    .resolve(recipientRoutingId)
                    .routes
                    .maxByOrNull { route -> route.generation }
            }.getOrNull()
                ?: return entry

        if (canonicalRoute.routingId == recipientRoutingId) {
            return entry
        }

        return queue.bindRecipient(
            envelopeId = entry.envelope.envelopeId,
            recipientDeviceRoutingId = canonicalRoute.routingId
        ) ?: entry
    }

    private fun retryDelay(completedAttempts: Int): Long {
        val shift = completedAttempts.coerceIn(0, MAXIMUM_BACKOFF_SHIFT)
        val multiplier = 1L shl shift
        if (retryBaseDelayMilliseconds > retryMaximumDelayMilliseconds / multiplier) {
            return retryMaximumDelayMilliseconds
        }
        return retryBaseDelayMilliseconds * multiplier
    }

    private suspend fun routeOnline(envelope: FederatedEnvelope): FederationAcknowledgement? {
        val localRoutingId =
            runCatching {
                localRouteResolver.resolve(envelope.recipientDeviceRoutingId)
            }.getOrNull()
        if (localRoutingId != null) {
            val acknowledgement =
                runCatching {
                    localGateway.deliver(
                        envelope.copy(recipientDeviceRoutingId = localRoutingId)
                    )
                }.getOrNull()
            if (acknowledgement?.state == EnvelopeAcceptanceState.STORED_AT_DESTINATION) {
                return acknowledgement
            }
        }

        peerRouter.routeEnvelope(envelope)?.let { acknowledgement ->
            return acknowledgement
        }

        val routes =
            runCatching {
                presenceDirectory.resolve(envelope.recipientDeviceRoutingId).routes
            }.getOrDefault(emptyList())
        for (route in routes.sortedByDescending { it.generation }) {
            val acknowledgement =
                runCatching {
                    if (route.nodeId == localNodeId) {
                        localGateway.deliver(
                            envelope.copy(recipientDeviceRoutingId = route.routingId)
                        )
                    } else {
                        val descriptor = nodeRegistry.find(route.nodeId) ?: return@runCatching null
                        remoteFederation.deliver(
                            descriptor,
                            envelope.copy(recipientDeviceRoutingId = route.routingId)
                        )
                    }
                }.getOrNull()

            if (acknowledgement?.state == EnvelopeAcceptanceState.STORED_AT_DESTINATION) {
                return acknowledgement
            }
        }
        return null
    }

    private suspend fun storeInMailbox(envelope: FederatedEnvelope): FederationAcknowledgement? =
        envelope.mailboxRoute?.let { route ->
            val descriptor = nodeRegistry.find(route.nodeId) ?: return@let null
            if (descriptor.mailboxEndpoint != route.nodeEndpoint) {
                return@let null
            }
            runCatching { mailbox.store(envelope) }.getOrNull()
        }

    private companion object {
        const val MAXIMUM_BACKOFF_SHIFT = 20
        const val BOOTSTRAP_ROUTING_ID_PREFIX = "scphone1_"
    }
}
