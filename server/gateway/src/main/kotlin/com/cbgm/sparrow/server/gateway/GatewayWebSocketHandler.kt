package com.cbgm.sparrow.server.gateway

import com.cbgm.sparrow.server.protocol.EnvelopeAcceptanceState
import com.cbgm.sparrow.server.protocol.FederatedEnvelope
import com.cbgm.sparrow.server.protocol.FederatedIndicatorEvent
import com.cbgm.sparrow.server.protocol.GatewayServerMessage
import com.cbgm.sparrow.server.protocol.TransportEnvelope
import io.ktor.server.websocket.DefaultWebSocketServerSession

class GatewayWebSocketHandler(
    private val nodeId: String,
    private val connections: ConnectionRegistry,
    private val federation: FederationClient,
    presence: PresenceClient,
    private val legacyPush: LegacyPushClient,
    private val routeLifetimeMilliseconds: Long,
    private val blobUploadTicketIssuer: GatewayBlobUploadTicketIssuer
) {
    private val bestEffortPresence = BestEffortPresenceClient(presence)
    private val pushDispatcher =
        GatewayPushDispatcher(
            pushClient = legacyPush,
            markFederationStored = federation::markStored
        )

    suspend fun handle(session: DefaultWebSocketServerSession) {
        GatewaySessionHandler(
            nodeId = nodeId,
            connections = connections,
            presence = bestEffortPresence,
            pushActions =
                GatewayPushActions(
                    deliverPending = pushDispatcher::deliverPending,
                    acknowledge = pushDispatcher::acknowledge
                ),
            routeValidator = GatewayRouteValidator(routeLifetimeMilliseconds),
            actions =
                GatewayMessageActions(
                    sendEnvelope = { connection, message ->
                        sendEnvelope(connection, message.envelope)
                    },
                    sendFederatedEnvelope = { connection, message ->
                        sendFederatedEnvelope(connection, message.envelope)
                    },
                    deliverIndicator = { connection, message ->
                        deliverIndicator(
                            sender = connection,
                            recipientId = message.recipientId,
                            indicatorType = message.indicatorType
                        )
                    },
                    issueBlobUploadTicket = { connection, message ->
                        connection.send(blobUploadTicketIssuer.issue(message))
                    }
                )
        ).handle(session)
    }

    fun close() {
        bestEffortPresence.close()
        pushDispatcher.close()
    }

    suspend fun acceptIncoming(
        envelope: FederatedEnvelope
    ): Boolean =
        storeAndDeliver(
            envelope = envelope,
            recipients =
                connections.find(
                    routingId = envelope.recipientDeviceRoutingId
                )
        )

    private suspend fun sendEnvelope(
        sender: GatewayConnection,
        envelope: TransportEnvelope
    ) {
        if (!sender.acceptsSenderRoutingId(envelope.senderId)) {
            sender.send(
                GatewayServerMessage.Error(
                    code = "SENDER_MISMATCH",
                    message = "Envelope sender differs from connection"
                )
            )
            return
        }

        val routedEnvelope = envelope.toFederatedEnvelope()
        val accepted =
            storeAndRouteFederatedEnvelope(
                envelope = routedEnvelope,
                pushStorage = legacyPush::store,
                networkDelivery = ::routeEnvelope,
                markFederationStored = federation::markStored
            )

        respondToEnvelope(
            sender = sender,
            envelopeId = envelope.envelopeId,
            expiresAtEpochMilliseconds = routedEnvelope.expiresAtEpochMilliseconds,
            accepted = accepted
        )
    }

    private suspend fun sendFederatedEnvelope(
        sender: GatewayConnection,
        envelope: FederatedEnvelope
    ) {
        if (!sender.acceptsSenderRoutingId(envelope.senderRoutingId)) {
            sender.send(
                GatewayServerMessage.Error(
                    code = "SENDER_MISMATCH",
                    message = "Envelope sender differs from connection"
                )
            )
            return
        }

        val routedEnvelope = envelope.withServerDeliveryDeadline()
        val accepted =
            storeAndRouteFederatedEnvelope(
                envelope = routedEnvelope,
                pushStorage = legacyPush::store,
                networkDelivery = ::routeEnvelope,
                markFederationStored = federation::markStored
            )

        respondToEnvelope(
            sender = sender,
            envelopeId = envelope.envelopeId,
            expiresAtEpochMilliseconds = routedEnvelope.expiresAtEpochMilliseconds,
            accepted = accepted
        )
    }

    private suspend fun respondToEnvelope(
        sender: GatewayConnection,
        envelopeId: String,
        expiresAtEpochMilliseconds: Long,
        accepted: Boolean
    ) {
        if (accepted) {
            sender.send(
                GatewayServerMessage.EnvelopeAccepted(
                    envelopeId = envelopeId,
                    expiresAtEpochMilliseconds = expiresAtEpochMilliseconds
                )
            )
        } else {
            sender.send(
                GatewayServerMessage.Error(
                    code = "ENVELOPE_REJECTED",
                    message = "Envelope could not be stored or queued"
                )
            )
        }
    }

    private suspend fun deliverIndicator(
        sender: GatewayConnection,
        recipientId: String,
        indicatorType: String
    ) {
        val event =
            FederatedIndicatorEvent(
                senderRoutingId = sender.routingId,
                recipientRoutingId = recipientId,
                indicatorType = indicatorType
            )

        routeFederatedIndicatorEvent(
            event = event,
            localDelivery = ::acceptIncomingIndicator,
            federation = federation
        )
    }

    suspend fun acceptIncomingIndicator(event: FederatedIndicatorEvent): Boolean {
        val recipients = connections.find(event.recipientRoutingId)
        if (recipients.isEmpty()) {
            return false
        }

        recipients.forEach { recipient ->
            runCatching {
                recipient.send(
                    GatewayServerMessage.IndicatorState(
                        senderId = event.senderRoutingId,
                        indicatorType = event.indicatorType
                    )
                )
            }
        }
        return true
    }

    private suspend fun acceptLocallyIfConnected(
        envelope: FederatedEnvelope
    ): Boolean {
        val recipients =
            connections.find(
                routingId = envelope.recipientDeviceRoutingId
            )

        if (recipients.isEmpty()) {
            return false
        }

        return storeAndDeliver(
            envelope = envelope,
            recipients = recipients
        )
    }

    private suspend fun routeEnvelope(
        envelope: FederatedEnvelope
    ): EnvelopeAcceptanceState? =
        routeFederatedEnvelope(
            envelope = envelope,
            localDelivery = ::acceptLocallyIfConnected,
            federation = federation
        )

    private suspend fun storeAndDeliver(
        envelope: FederatedEnvelope,
        recipients: List<GatewayConnection>
    ): Boolean {
        val transportEnvelope = envelope.toTransportEnvelope()
        val deliveredLive =
            recipients.any { recipient ->
                runCatching {
                    recipient.send(
                        GatewayServerMessage.IncomingEnvelope(
                            envelope = transportEnvelope
                        )
                    )
                }.isSuccess
            }

        if (deliveredLive) {
            return true
        }

        return runCatching {
            legacyPush.store(transportEnvelope)
        }.getOrDefault(false)
    }
}

internal suspend fun routeFederatedEnvelope(
    envelope: FederatedEnvelope,
    localDelivery: suspend (FederatedEnvelope) -> Boolean,
    federation: FederationClient
): EnvelopeAcceptanceState? {
    if (localDelivery(envelope)) {
        return EnvelopeAcceptanceState.STORED_AT_DESTINATION
    }

    return runCatching { federation.route(envelope).state }.getOrNull()
}

internal suspend fun storeAndRouteLegacyEnvelope(
    envelope: TransportEnvelope,
    pushStorage: suspend (TransportEnvelope) -> Boolean,
    networkDelivery: suspend (FederatedEnvelope) -> EnvelopeAcceptanceState?,
    markFederationStored: suspend (String) -> Unit
): Boolean =
    storeAndRouteEnvelope(
        pushEnvelope = envelope,
        networkEnvelope = envelope.toFederatedEnvelope(),
        networkDelivery = networkDelivery,
        fallbackActions =
            EnvelopeFallbackActions(
                pushStorage = pushStorage,
                markFederationStored = markFederationStored
            )
    )

internal suspend fun storeAndRouteFederatedEnvelope(
    envelope: FederatedEnvelope,
    pushStorage: suspend (TransportEnvelope) -> Boolean,
    networkDelivery: suspend (FederatedEnvelope) -> EnvelopeAcceptanceState?,
    markFederationStored: suspend (String) -> Unit
): Boolean =
    storeAndRouteEnvelope(
        pushEnvelope = envelope.toTransportEnvelope(),
        networkEnvelope = envelope,
        networkDelivery = networkDelivery,
        fallbackActions =
            EnvelopeFallbackActions(
                pushStorage = pushStorage,
                markFederationStored = markFederationStored
            )
    )

private data class EnvelopeFallbackActions(
    val pushStorage: suspend (TransportEnvelope) -> Boolean,
    val markFederationStored: suspend (String) -> Unit
)

private suspend fun storeAndRouteEnvelope(
    pushEnvelope: TransportEnvelope,
    networkEnvelope: FederatedEnvelope,
    networkDelivery: suspend (FederatedEnvelope) -> EnvelopeAcceptanceState?,
    fallbackActions: EnvelopeFallbackActions
): Boolean {
    val routingState =
        runCatching {
            networkDelivery(networkEnvelope)
        }.getOrNull()

    if (routingState == EnvelopeAcceptanceState.STORED_AT_DESTINATION) {
        return true
    }

    // QUEUED_AT_GATEWAY is not proof of durable recipient storage. In particular,
    // the federation queue can be memory-backed. A fire-and-forget push fallback
    // previously sent success to the client *before* push storage ran, so a failure
    // could leave a message reported as SENT but unavailable to an offline recipient.
    // Store synchronously and acknowledge only after the pending inbox accepts it.
    val storedForPush =
        runCatching {
            fallbackActions.pushStorage(pushEnvelope)
        }.getOrDefault(false)

    if (storedForPush) {
        // This is bookkeeping for the separately queued federation copy. If it
        // fails, the encrypted envelope is nevertheless already in push storage.
        runCatching {
            fallbackActions.markFederationStored(networkEnvelope.envelopeId)
        }
    }

    // When neither federation has stored it at the recipient nor the push inbox
    // can accept it, report failure. The sender's outbox will retain/retry it.
    return storedForPush
}

internal suspend fun routeFederatedIndicatorEvent(
    event: FederatedIndicatorEvent,
    localDelivery: suspend (FederatedIndicatorEvent) -> Boolean,
    federation: FederationClient
): Boolean {
    if (localDelivery(event)) {
        return true
    }

    return runCatching {
        federation.routeIndicator(event)
    }.getOrDefault(false)
}

internal fun TransportEnvelope.toFederatedEnvelope(
    nowEpochMilliseconds: Long = System.currentTimeMillis()
): FederatedEnvelope =
    FederatedEnvelope(
        envelopeId = envelopeId,
        senderRoutingId = senderId,
        recipientDeviceRoutingId = recipientId,
        mailboxRoute = null,
        encryptedPayload = payload,
        createdAtEpochMilliseconds = createdAtEpochMilliseconds,
        expiresAtEpochMilliseconds =
            maxOf(nowEpochMilliseconds, createdAtEpochMilliseconds) +
                if (recipientId.startsWith(BOOTSTRAP_ROUTING_ID_PREFIX)) {
                    BOOTSTRAP_ENVELOPE_TTL_MILLISECONDS
                } else {
                    DEFAULT_ENVELOPE_TTL_MILLISECONDS
                }
    )

private fun FederatedEnvelope.withServerDeliveryDeadline(
    nowEpochMilliseconds: Long = System.currentTimeMillis()
): FederatedEnvelope {
    val serverDeadline =
        maxOf(nowEpochMilliseconds, createdAtEpochMilliseconds) + DEFAULT_ENVELOPE_TTL_MILLISECONDS
    return copy(
        expiresAtEpochMilliseconds = minOf(expiresAtEpochMilliseconds, serverDeadline)
    )
}

private fun FederatedEnvelope.toTransportEnvelope(): TransportEnvelope =
    TransportEnvelope(
        envelopeId = envelopeId,
        senderId = senderRoutingId,
        recipientId = recipientDeviceRoutingId,
        payload = encryptedPayload,
        createdAtEpochMilliseconds = createdAtEpochMilliseconds
    )

private const val BOOTSTRAP_ROUTING_ID_PREFIX = "scphone1_"
private const val BOOTSTRAP_ENVELOPE_TTL_MILLISECONDS = 24L * 60L * 60L * 1_000L
private const val DEFAULT_ENVELOPE_TTL_MILLISECONDS = 7L * 24L * 60L * 60L * 1_000L
