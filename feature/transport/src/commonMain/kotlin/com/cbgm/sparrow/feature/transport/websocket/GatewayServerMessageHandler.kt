package com.cbgm.sparrow.feature.transport.websocket

import com.cbgm.sparrow.core.logging.SparrowLog
import com.cbgm.sparrow.feature.transport.gateway.model.GatewayBlobUploadTicket
import com.cbgm.sparrow.feature.transport.gateway.model.GatewayEnvelopeAcceptance
import com.cbgm.sparrow.feature.transport.gateway.model.GatewayServerMessage
import com.cbgm.sparrow.feature.transport.gateway.model.GatewayTypingEvent
import com.cbgm.sparrow.feature.transport.gateway.model.TransportEnvelope
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.json.Json

@OptIn(InternalSerializationApi::class)
internal class GatewayServerMessageHandler(
    private val json: Json,
    private val pendingRequestRegistry: GatewayPendingRequestRegistry
) {
    private val logger = SparrowLog.withTag("GatewayServerMessageHandler")

    suspend fun handle(
        encodedMessage: String,
        expectedRoutingId: String,
        onGatewayRegistered: () -> Unit,
        onRouteRegistered: (Set<String>) -> Unit,
        onRouteRejected: (Throwable) -> Unit,
        onIncomingEnvelope: suspend (TransportEnvelope) -> Unit,
        onTypingEvent: suspend (GatewayTypingEvent) -> Unit
    ) {
        val message =
            runCatching {
                json.decodeFromString<GatewayServerMessage>(encodedMessage)
            }.getOrElse { error ->
                logger.error(error) { "Invalid gateway response" }
                throw IllegalStateException("Invalid gateway response", error)
            }

        when (message) {
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

            is GatewayServerMessage.TypingState -> {
                onTypingEvent(
                    GatewayTypingEvent(
                        senderId = message.senderId,
                        isTyping = message.isTyping
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
                logger.warn { "Gateway error ${message.code}: ${message.message}" }
                handleGatewayError(message, onRouteRejected)
            }
        }
    }

    private fun handleGatewayError(
        message: GatewayServerMessage.Error,
        onRouteRejected: (Throwable) -> Unit
    ) {
        when (message.code) {
            "INVALID_ROUTE_REFRESH",
            "ROUTE_REJECTED" ->
                onRouteRejected(
                    IllegalStateException(
                        "Presence route rejected by gateway: ${message.code}"
                    )
                )

            "INVALID_ROUTE",
            "ALREADY_REGISTERED" ->
                throw IllegalStateException(
                    "Gateway registration rejected: ${message.code}"
                )

            else -> Unit
        }
    }
}
