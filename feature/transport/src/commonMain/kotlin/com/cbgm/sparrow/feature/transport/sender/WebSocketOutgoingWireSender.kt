package com.cbgm.sparrow.feature.transport.sender

import com.cbgm.sparrow.core.id.IdGenerator
import com.cbgm.sparrow.core.protocol.mailbox.MailboxRouteRepository
import com.cbgm.sparrow.core.protocol.transport.OutgoingWireSender
import com.cbgm.sparrow.core.time.SystemClock
import com.cbgm.sparrow.feature.transport.config.TransportConfig
import com.cbgm.sparrow.feature.transport.gateway.model.FederatedEnvelope
import com.cbgm.sparrow.feature.transport.gateway.model.TransportEnvelope
import com.cbgm.sparrow.feature.transport.routing.LocalBootstrapRoutingIdProvider
import com.cbgm.sparrow.feature.transport.routing.LocalRoutingIdProvider
import com.cbgm.sparrow.feature.transport.websocket.WebSocketTransportClient

class WebSocketOutgoingWireSender(
    private val webSocketTransportClient: WebSocketTransportClient,
    private val localRoutingIdProvider: LocalRoutingIdProvider,
    private val localBootstrapRoutingIdProvider: LocalBootstrapRoutingIdProvider,
    private val transportConfig: TransportConfig,
    private val mailboxRouteRepository: MailboxRouteRepository? = null
) : OutgoingWireSender {
    override suspend fun send(
        recipientAddress: String,
        encodedTransportPayload: String
    ): Result<Unit> =
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
                            ).getOrThrow()
                    }
                } else {
                    localRoutingIdProvider.getLocalRoutingId().getOrThrow()
                }

            val envelopeId = IdGenerator.generate()
            val createdAt = SystemClock.nowEpochMilliseconds()
            val route =
                mailboxRouteRepository
                    ?.remoteForRecipientRoutingId(recipientAddress)
                    ?.getOrThrow()
                    ?.takeIf { it.expiresAtEpochMilliseconds > createdAt }

            if (route == null) {
                webSocketTransportClient
                    .sendEnvelopeAndAwaitAcceptance(
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
            } else {
                webSocketTransportClient
                    .sendFederatedEnvelopeAndAwaitAcceptance(
                        envelope =
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
                            ),
                        timeoutMilliseconds = transportConfig.acknowledgementTimeoutMilliseconds
                    ).getOrThrow()
            }
        }

    private companion object {
        const val ENVELOPE_LIFETIME_MILLISECONDS = 7L * 24L * 60L * 60L * 1_000L
        const val BOOTSTRAP_ROUTING_ID_PREFIX = "scphone1_"
    }
}
