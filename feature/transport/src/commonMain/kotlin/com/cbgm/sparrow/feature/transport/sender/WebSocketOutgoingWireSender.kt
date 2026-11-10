package com.cbgm.sparrow.feature.transport.sender

import com.cbgm.sparrow.core.util.id.IdGenerator
import com.cbgm.sparrow.core.util.logging.SparrowLog
import com.cbgm.sparrow.core.util.time.SystemClock
import com.cbgm.sparrow.feature.transport.circuit.DirectSendingPolicy
import com.cbgm.sparrow.feature.transport.config.TransportConfig
import com.cbgm.sparrow.feature.transport.gateway.model.FederatedEnvelope
import com.cbgm.sparrow.feature.transport.gateway.model.TransportEnvelope
import com.cbgm.sparrow.feature.transport.mailbox.MailboxGateway
import com.cbgm.sparrow.feature.transport.routing.LocalBootstrapRoutingIdProvider
import com.cbgm.sparrow.feature.transport.routing.LocalRoutingIdProvider
import com.cbgm.sparrow.feature.transport.websocket.WebSocketTransportClient
import com.cbgm.sparrow.protocol.mailbox.MailboxRouteRepository
import com.cbgm.sparrow.protocol.transport.OutgoingWireAcceptance
import com.cbgm.sparrow.protocol.transport.OutgoingWireSender
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException

class WebSocketOutgoingWireSender(
    private val webSocketTransportClient: WebSocketTransportClient,
    private val localRoutingIdProvider: LocalRoutingIdProvider,
    private val localBootstrapRoutingIdProvider: LocalBootstrapRoutingIdProvider,
    private val transportConfig: TransportConfig,
    private val mailboxRouteRepository: MailboxRouteRepository? = null,
    private val mailboxGateway: MailboxGateway? = null,
    private val sendingPolicy: DirectSendingPolicy? = null
) : OutgoingWireSender {
    private val logger = SparrowLog.withTag("WebSocketOutgoingWireSender")

    override suspend fun send(
        recipientAddress: String,
        encodedTransportPayload: String
    ): Result<Unit> =
        sendWithAcceptance(recipientAddress, encodedTransportPayload, recipientContactId = null).map { }

    override suspend fun sendWithAcceptance(
        recipientAddress: String,
        encodedTransportPayload: String,
        recipientContactId: String?
    ): Result<OutgoingWireAcceptance> =
        runCatching {
            require(recipientAddress.isNotBlank()) { "Recipient address must not be blank" }
            require(encodedTransportPayload.isNotBlank()) { "Transport payload must not be blank" }

            val usesBootstrapRouting = recipientAddress.startsWith(BOOTSTRAP_ROUTING_ID_PREFIX)
            val senderRoutingId =
                if (usesBootstrapRouting) {
                    localBootstrapRoutingIdProvider.getLocalBootstrapRoutingId().getOrThrow().also { alias ->
                        webSocketTransportClient
                            .awaitRoutingAlias(
                                routingAlias = alias,
                                timeoutMilliseconds = transportConfig.acknowledgementTimeoutMilliseconds
                            ).getOrElse { failure ->
                                if (failure is CancellationException && failure !is TimeoutCancellationException) throw failure
                                // A first invite names the sender's phone alias, which a Control Plane must hold.
                                logger.warn { "First invite not sent: own phone number is not bound at any Control Plane" }
                                throw IllegalStateException("Own phone number is not bound at any Control Plane yet")
                            }
                    }
                } else {
                    localRoutingIdProvider.getLocalRoutingId().getOrThrow()
                }

            val envelopeId = IdGenerator.generate()
            val createdAt = SystemClock.nowEpochMilliseconds()
            // By contact when it is known: the routing table holds only the phone alias, not the address messages to
            // a verified contact are sent to, so a lookup by that address would never find the route.
            val route =
                mailboxRouteRepository
                    ?.let { routes -> recipientContactId?.let { routes.remoteForContact(it) } ?: routes.remoteForRecipientRoutingId(recipientAddress) }
                    ?.getOrThrow()
                    ?.takeIf { it.expiresAtEpochMilliseconds > createdAt }

            if (route == null) {
                // Without a mailbox route the message cannot use the circuit; the connected node learns the recipient.
                sendingPolicy?.checkUnprotectedSendAllowed()
                logger.debug { "Message sent through the connected node (no mailbox route for this contact yet)" }
            }
            val expiresAt =
                when (route) {
                    null ->
                        webSocketTransportClient
                            .sendEnvelopeAndAwaitServerAcceptance(
                                envelope =
                                    TransportEnvelope(
                                        envelopeId = envelopeId,
                                        senderId = senderRoutingId,
                                        recipientId = recipientAddress,
                                        payload = encodedTransportPayload,
                                        createdAtEpochMilliseconds = createdAt
                                    ),
                                timeoutMilliseconds = transportConfig.acknowledgementTimeoutMilliseconds
                            ).getOrThrow()
                            .expiresAtEpochMilliseconds

                    else ->
                        sendToMailbox(
                            FederatedEnvelope(
                                envelopeId = envelopeId,
                                senderRoutingId = senderRoutingId,
                                recipientDeviceRoutingId = recipientAddress,
                                mailboxRoute = route,
                                encryptedPayload = encodedTransportPayload,
                                createdAtEpochMilliseconds = createdAt,
                                expiresAtEpochMilliseconds =
                                    minOf(
                                        createdAt + ENVELOPE_LIFETIME_MILLISECONDS,
                                        route.expiresAtEpochMilliseconds
                                    )
                            )
                        )
                }

            OutgoingWireAcceptance(expiresAtEpochMilliseconds = expiresAt)
        }

    /**
     * Puts the envelope into the contact's mailbox itself, through the circuit ([MailboxGateway] uses it), so the
     * connected node does not learn whom the device writes to. When the mailbox cannot be reached and the user allows
     * sending directly, the connected node delivers it as before (live first, then the mailbox).
     */
    private suspend fun sendToMailbox(envelope: FederatedEnvelope): Long {
        val gateway = mailboxGateway ?: return throughConnectedNode(envelope)
        gateway.deliver(envelope.withoutRoutingIds()).exceptionOrNull()?.let { failure ->
            if (failure is CancellationException) throw failure
            if (sendingPolicy?.sendsDirectly() == false) throw failure
            logger.warn { "Message sent through the connected node (contact's mailbox not reachable: ${failure.message}): that node sees the recipient" }
            return throughConnectedNode(envelope)
        }
        logger.debug { "Message put into the contact's mailbox" }
        return envelope.expiresAtEpochMilliseconds
    }

    private suspend fun throughConnectedNode(envelope: FederatedEnvelope): Long =
        webSocketTransportClient
            .sendFederatedEnvelopeAndAwaitServerAcceptance(
                envelope = envelope,
                timeoutMilliseconds = transportConfig.acknowledgementTimeoutMilliseconds
            ).getOrThrow()
            .expiresAtEpochMilliseconds

    private companion object {
        const val ENVELOPE_LIFETIME_MILLISECONDS = 7L * 24L * 60L * 60L * 1_000L
        const val BOOTSTRAP_ROUTING_ID_PREFIX = "scphone1_"
    }
}
