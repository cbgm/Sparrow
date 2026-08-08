package com.cbgm.securechat.server.gateway

import com.cbgm.securechat.server.protocol.EnvelopeAcceptanceState
import com.cbgm.securechat.server.protocol.FederatedEnvelope
import com.cbgm.securechat.server.protocol.FederatedTypingEvent
import com.cbgm.securechat.server.protocol.GatewayServerMessage
import com.cbgm.securechat.server.protocol.RelayEnvelope
import io.ktor.server.websocket.DefaultWebSocketServerSession

class GatewayWebSocketHandler(
    private val nodeId: String,
    private val connections: ConnectionRegistry,
    private val federation: FederationClient,
    private val presence: PresenceClient,
    private val legacyPush: LegacyPushClient,
    private val routeLifetimeMilliseconds: Long
) {
    suspend fun handle(session: DefaultWebSocketServerSession) {
        GatewaySessionHandler(
            nodeId = nodeId,
            connections = connections,
            presence = presence,
            legacyPush = legacyPush,
            routeValidator = GatewayRouteValidator(routeLifetimeMilliseconds),
            actions =
                GatewayMessageActions(
                    sendEnvelope = { connection, message ->
                        sendEnvelope(connection, message.envelope)
                    },
                    sendFederatedEnvelope = { connection, message ->
                        sendFederatedEnvelope(connection, message.envelope)
                    },
                    deliverTyping = { connection, message ->
                        deliverTyping(
                            sender = connection,
                            recipientId = message.recipientId,
                            isTyping = message.isTyping
                        )
                    }
                )
        ).handle(session)
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
        envelope: RelayEnvelope
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

        val accepted =
            storeAndRouteLegacyEnvelope(
                envelope = envelope,
                pushStorage = legacyPush::store,
                networkDelivery = ::routeEnvelope,
                markFederationStored = federation::markStored
            )

        respondToEnvelope(
            sender = sender,
            envelopeId = envelope.envelopeId,
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

        val accepted =
            storeAndRouteFederatedEnvelope(
                envelope = envelope,
                pushStorage = legacyPush::store,
                networkDelivery = ::routeEnvelope,
                markFederationStored = federation::markStored
            )

        respondToEnvelope(
            sender = sender,
            envelopeId = envelope.envelopeId,
            accepted = accepted
        )
    }

    private suspend fun respondToEnvelope(
        sender: GatewayConnection,
        envelopeId: String,
        accepted: Boolean
    ) {
        if (accepted) {
            sender.send(
                GatewayServerMessage.EnvelopeAccepted(
                    envelopeId = envelopeId
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

    private suspend fun deliverTyping(
        sender: GatewayConnection,
        recipientId: String,
        isTyping: Boolean
    ) {
        val event =
            FederatedTypingEvent(
                senderRoutingId = sender.routingId,
                recipientRoutingId = recipientId,
                isTyping = isTyping
            )

        routeFederatedTypingEvent(
            event = event,
            localDelivery = ::acceptIncomingTyping,
            federation = federation
        )
    }

    suspend fun acceptIncomingTyping(event: FederatedTypingEvent): Boolean {
        val recipients = connections.find(event.recipientRoutingId)
        if (recipients.isEmpty()) {
            return false
        }

        recipients.forEach { recipient ->
            runCatching {
                recipient.send(
                    GatewayServerMessage.TypingState(
                        senderId = event.senderRoutingId,
                        isTyping = event.isTyping
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

    private suspend fun routeEnvelope(envelope: FederatedEnvelope): Boolean =
        routeFederatedEnvelope(
            envelope = envelope,
            localDelivery = ::acceptLocallyIfConnected,
            federation = federation
        )

    private suspend fun storeAndDeliver(
        envelope: FederatedEnvelope,
        recipients: List<GatewayConnection>
    ): Boolean {
        val relayEnvelope = envelope.toRelayEnvelope()

        val stored =
            runCatching {
                legacyPush.store(relayEnvelope)
            }.getOrDefault(false)

        val delivered =
            recipients.any { recipient ->
                runCatching {
                    recipient.send(
                        GatewayServerMessage.IncomingEnvelope(
                            envelope = relayEnvelope
                        )
                    )
                }.isSuccess
            }

        return stored || delivered
    }
}

internal suspend fun routeFederatedEnvelope(
    envelope: FederatedEnvelope,
    localDelivery: suspend (FederatedEnvelope) -> Boolean,
    federation: FederationClient
): Boolean {
    if (localDelivery(envelope)) {
        return true
    }

    return federation.route(envelope).state in
        setOf(
            EnvelopeAcceptanceState.QUEUED_AT_GATEWAY,
            EnvelopeAcceptanceState.STORED_AT_DESTINATION
        )
}

internal suspend fun storeAndRouteLegacyEnvelope(
    envelope: RelayEnvelope,
    pushStorage: suspend (RelayEnvelope) -> Boolean,
    networkDelivery: suspend (FederatedEnvelope) -> Boolean,
    markFederationStored: suspend (String) -> Unit
): Boolean =
    storeAndRouteEnvelope(
        pushEnvelope = envelope,
        networkEnvelope = envelope.toFederatedEnvelope(),
        pushStorage = pushStorage,
        networkDelivery = networkDelivery,
        markFederationStored = markFederationStored
    )

internal suspend fun storeAndRouteFederatedEnvelope(
    envelope: FederatedEnvelope,
    pushStorage: suspend (RelayEnvelope) -> Boolean,
    networkDelivery: suspend (FederatedEnvelope) -> Boolean,
    markFederationStored: suspend (String) -> Unit
): Boolean =
    storeAndRouteEnvelope(
        pushEnvelope = envelope.toRelayEnvelope(),
        networkEnvelope = envelope,
        pushStorage = pushStorage,
        networkDelivery = networkDelivery,
        markFederationStored = markFederationStored
    )

private suspend fun storeAndRouteEnvelope(
    pushEnvelope: RelayEnvelope,
    networkEnvelope: FederatedEnvelope,
    pushStorage: suspend (RelayEnvelope) -> Boolean,
    networkDelivery: suspend (FederatedEnvelope) -> Boolean,
    markFederationStored: suspend (String) -> Unit
): Boolean {
    val storedForPush =
        runCatching {
            pushStorage(pushEnvelope)
        }.getOrDefault(false)

    val routedOnline =
        runCatching {
            networkDelivery(networkEnvelope)
        }.getOrDefault(false)

    if (storedForPush) {
        runCatching {
            markFederationStored(networkEnvelope.envelopeId)
        }
    }

    return storedForPush || routedOnline
}

internal suspend fun routeFederatedTypingEvent(
    event: FederatedTypingEvent,
    localDelivery: suspend (FederatedTypingEvent) -> Boolean,
    federation: FederationClient
): Boolean {
    if (localDelivery(event)) {
        return true
    }

    return runCatching {
        federation.routeTyping(event)
    }.getOrDefault(false)
}

internal fun RelayEnvelope.toFederatedEnvelope(): FederatedEnvelope =
    FederatedEnvelope(
        envelopeId = envelopeId,
        senderRoutingId = senderId,
        recipientDeviceRoutingId = recipientId,
        mailboxRoute = null,
        encryptedPayload = payload,
        createdAtEpochMilliseconds = createdAtEpochMilliseconds,
        expiresAtEpochMilliseconds =
            createdAtEpochMilliseconds +
                if (recipientId.startsWith(BOOTSTRAP_ROUTING_ID_PREFIX)) {
                    BOOTSTRAP_ENVELOPE_TTL_MILLISECONDS
                } else {
                    DEFAULT_ENVELOPE_TTL_MILLISECONDS
                }
    )

private fun FederatedEnvelope.toRelayEnvelope(): RelayEnvelope =
    RelayEnvelope(
        envelopeId = envelopeId,
        senderId = senderRoutingId,
        recipientId = recipientDeviceRoutingId,
        payload = encryptedPayload,
        createdAtEpochMilliseconds = createdAtEpochMilliseconds
    )

private const val BOOTSTRAP_ROUTING_ID_PREFIX = "scphone1_"
private const val BOOTSTRAP_ENVELOPE_TTL_MILLISECONDS = 24L * 60L * 60L * 1_000L
private const val DEFAULT_ENVELOPE_TTL_MILLISECONDS = 7L * 24L * 60L * 60L * 1_000L
