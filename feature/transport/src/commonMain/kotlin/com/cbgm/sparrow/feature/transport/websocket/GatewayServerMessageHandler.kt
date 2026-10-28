package com.cbgm.sparrow.feature.transport.websocket

import com.cbgm.sparrow.core.util.logging.SparrowLog
import com.cbgm.sparrow.feature.transport.gateway.model.GatewayBlobUploadTicket
import com.cbgm.sparrow.feature.transport.gateway.model.GatewayEnvelopeAcceptance
import com.cbgm.sparrow.feature.transport.gateway.model.GatewayIndicatorEvent
import com.cbgm.sparrow.feature.transport.gateway.model.GatewayServerMessage
import com.cbgm.sparrow.feature.transport.gateway.model.TransportEnvelope
import com.cbgm.sparrow.feature.transport.presence.PresenceRouteRefreshRejectedException
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.json.Json

@OptIn(InternalSerializationApi::class)
internal class GatewayServerMessageHandler(
    private val json: Json,
    private val pendingRequestRegistry: GatewayPendingRequestRegistry
) {
    private val logger = SparrowLog.withTag("GatewayServerMessageHandler")

    /**
     * Reads the first gateway frame. Sign-in needs the challenge before anything else, so any other
     * frame fails the connection and the normal reconnect path takes over.
     */
    fun decodeChallenge(encodedMessage: String): GatewayServerMessage.Challenge =
        when (val message = decode(encodedMessage)) {
            is GatewayServerMessage.Challenge -> message
            is GatewayServerMessage.Error ->
                throw IllegalStateException("Gateway rejected the session: ${message.code}")

            else -> throw IllegalStateException("Gateway did not send a session challenge")
        }

    suspend fun handle(
        encodedMessage: String,
        expectedRoutingId: String,
        onGatewayRegistered: () -> Unit,
        onRouteRegistered: (Set<String>) -> Unit,
        onRouteRejected: (Throwable) -> Unit,
        onIncomingEnvelope: suspend (TransportEnvelope) -> Unit,
        onIndicatorEvent: suspend (GatewayIndicatorEvent) -> Unit
    ) {
        when (val message = decode(encodedMessage)) {
            is GatewayServerMessage.Challenge ->
                throw IllegalStateException("Gateway sent a second session challenge")

            is GatewayServerMessage.Registered -> {
                check(message.routingId == expectedRoutingId) {
                    "Gateway registered an unexpected routing identity"
                }
                logger.info { "Gateway registration accepted for ${message.routingId}" }
                onGatewayRegistered()
            }

            is GatewayServerMessage.RouteRegistered -> {
                onRouteRegistered(message.aliases.toSet())
            }

            is GatewayServerMessage.IncomingEnvelope -> {
                onIncomingEnvelope(message.envelope)
            }

            is GatewayServerMessage.IndicatorState -> {
                onIndicatorEvent(
                    GatewayIndicatorEvent(
                        senderId = message.senderId,
                        indicatorType = message.indicatorType
                    )
                )
            }

            is GatewayServerMessage.EnvelopeAccepted -> {
                pendingRequestRegistry.completeEnvelopeAcceptance(
                    GatewayEnvelopeAcceptance(
                        envelopeId = message.envelopeId,
                        expiresAtEpochMilliseconds = message.expiresAtEpochMilliseconds
                    )
                )
            }

            is GatewayServerMessage.BlobUploadTicketIssued -> {
                pendingRequestRegistry.completeBlobUploadTicket(
                    GatewayBlobUploadTicket(
                        requestId = message.requestId,
                        nodeId = message.nodeId,
                        uploadToken = message.uploadToken,
                        blobExpiresAtEpochMilliseconds = message.blobExpiresAtEpochMilliseconds
                    )
                )
            }

            is GatewayServerMessage.BlobUploadTicketRejected -> {
                pendingRequestRegistry.rejectBlobUploadTicket(
                    requestId = message.requestId,
                    error = IllegalStateException("${message.code}: ${message.message}")
                )
            }

            is GatewayServerMessage.Error -> {
                if (message.code == "INVALID_ROUTE_REFRESH" || message.code == "ROUTE_REJECTED") {
                    // Presence housekeeping can fail while message delivery still
                    // works. Reconnect/retry internally; no global error snackbar.
                    logger.debug { "Gateway presence response ${message.code}: ${message.message}" }
                } else {
                    logger.error { "Gateway error ${message.code}: ${message.message}" }
                }
                handleGatewayError(message, onRouteRejected)
            }
        }
    }

    private fun decode(encodedMessage: String): GatewayServerMessage =
        runCatching {
            json.decodeFromString<GatewayServerMessage>(encodedMessage)
        }.getOrElse { error ->
            logger.error(error) { "Invalid gateway response" }
            throw IllegalStateException("Invalid gateway response", error)
        }

    private fun handleGatewayError(
        message: GatewayServerMessage.Error,
        onRouteRejected: (Throwable) -> Unit
    ) {
        when (message.code) {
            "INVALID_ROUTE_REFRESH",
            "ROUTE_REJECTED" ->
                onRouteRejected(
                    PresenceRouteRefreshRejectedException(
                        code = message.code,
                        isExpiration = message.code == "INVALID_ROUTE_REFRESH" &&
                            message.message == "Signed route is invalid: EXPIRATION"
                    )
                )

            "INVALID_ROUTE",
            "INVALID_SESSION",
            "ALREADY_REGISTERED" ->
                throw IllegalStateException(
                    "Gateway registration rejected: ${message.code}"
                )

            else -> Unit
        }
    }
}
