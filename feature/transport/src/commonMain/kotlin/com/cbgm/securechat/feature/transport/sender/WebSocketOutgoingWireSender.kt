package com.cbgm.securechat.feature.transport.sender

import com.cbgm.securechat.core.id.IdGenerator
import com.cbgm.securechat.core.protocol.mailbox.MailboxRouteRepository
import com.cbgm.securechat.core.protocol.transport.OutgoingWireSender
import com.cbgm.securechat.core.time.SystemClock
import com.cbgm.securechat.feature.transport.relay.config.RelayTransportConfig
import com.cbgm.securechat.feature.transport.relay.identity.LocalBootstrapRelayIdProvider
import com.cbgm.securechat.feature.transport.relay.identity.LocalRelayIdProvider
import com.cbgm.securechat.feature.transport.relay.model.FederatedEnvelope
import com.cbgm.securechat.feature.transport.relay.model.RelayEnvelope
import com.cbgm.securechat.feature.transport.websocket.WebSocketTransportClient

class WebSocketOutgoingWireSender(
    private val webSocketTransportClient: WebSocketTransportClient,
    private val localRelayIdProvider: LocalRelayIdProvider,
    private val localBootstrapRelayIdProvider: LocalBootstrapRelayIdProvider,
    private val relayTransportConfig: RelayTransportConfig,
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
            val senderRelayId =
                if (usesBootstrapRouting) {
                    localBootstrapRelayIdProvider.getLocalBootstrapRelayId().getOrThrow().also { alias ->
                        webSocketTransportClient
                            .awaitRoutingAlias(
                                routingAlias = alias,
                                timeoutMilliseconds = relayTransportConfig.acknowledgementTimeoutMilliseconds
                            ).getOrThrow()
                    }
                } else {
                    localRelayIdProvider.getLocalRelayId().getOrThrow()
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
                            RelayEnvelope(
                                envelopeId = envelopeId,
                                senderId = senderRelayId,
                                recipientId = recipientAddress,
                                payload = encodedTransportPayload,
                                createdAtEpochMilliseconds = createdAt
                            ),
                        timeoutMilliseconds = relayTransportConfig.acknowledgementTimeoutMilliseconds
                    ).getOrThrow()
            } else {
                webSocketTransportClient
                    .sendFederatedEnvelopeAndAwaitAcceptance(
                        envelope =
                            FederatedEnvelope(
                                envelopeId = envelopeId,
                                senderRoutingId = senderRelayId,
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
                        timeoutMilliseconds = relayTransportConfig.acknowledgementTimeoutMilliseconds
                    ).getOrThrow()
            }
        }

    private companion object {
        const val ENVELOPE_LIFETIME_MILLISECONDS = 7L * 24L * 60L * 60L * 1_000L
        const val BOOTSTRAP_ROUTING_ID_PREFIX = "scphone1_"
    }
}
