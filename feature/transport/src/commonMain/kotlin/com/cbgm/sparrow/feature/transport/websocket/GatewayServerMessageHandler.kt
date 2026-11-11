package com.cbgm.sparrow.feature.transport.websocket

import com.cbgm.sparrow.core.util.logging.SparrowLog
import com.cbgm.sparrow.feature.transport.gateway.codec.envelopeIdOrNull
import com.cbgm.sparrow.feature.transport.gateway.model.GatewayEnvelopeAcceptance
import com.cbgm.sparrow.feature.transport.gateway.model.GatewayServerMessage
import com.cbgm.sparrow.feature.transport.gateway.model.TransportEnvelope
import com.cbgm.sparrow.feature.transport.presence.PresenceRouteRefreshRejectedException
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject

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
        when (val message = decodeOrNull(encodedMessage) ?: throw IllegalStateException("Invalid gateway response")) {
            is GatewayServerMessage.Challenge -> message
            is GatewayServerMessage.Error ->
                throw IllegalStateException("Gateway rejected the session: ${message.code}")

            else -> throw IllegalStateException("Gateway did not send a session challenge")
        }

    suspend fun handle(
        encodedMessage: String,
        expectedRoutingId: String,
        onGatewayRegistered: () -> Unit,
        onRouteRegistered: () -> Unit,
        onRouteRejected: (Throwable) -> Unit,
        onIncomingEnvelope: suspend (TransportEnvelope) -> Unit,
        onUnreadableEnvelope: suspend (String) -> Unit
    ) {
        // One unreadable frame, or a message type from a newer server, is skipped instead of ending the session.
        val message = decodeOrNull(encodedMessage) ?: return skipUnreadable(encodedMessage, onUnreadableEnvelope)
        when (message) {
            is GatewayServerMessage.Challenge ->
                throw IllegalStateException("Gateway sent a second session challenge")

            is GatewayServerMessage.Registered -> {
                check(message.routingId == expectedRoutingId) {
                    "Gateway registered an unexpected routing identity"
                }
                logger.info { "Gateway registration accepted for ${message.routingId}" }
                onGatewayRegistered()
            }

            GatewayServerMessage.RouteRegistered -> onRouteRegistered()

            is GatewayServerMessage.IncomingEnvelope -> {
                onIncomingEnvelope(message.envelope)
            }

            is GatewayServerMessage.EnvelopeAccepted -> {
                pendingRequestRegistry.completeEnvelopeAcceptance(
                    GatewayEnvelopeAcceptance(
                        envelopeId = message.envelopeId,
                        expiresAtEpochMilliseconds = message.expiresAtEpochMilliseconds
                    )
                )
            }

            is GatewayServerMessage.Error -> handleErrorMessage(message, onRouteRejected)
        }
    }

    private fun decodeOrNull(encodedMessage: String): GatewayServerMessage? =
        runCatching {
            json.decodeFromString<GatewayServerMessage>(encodedMessage)
        }.onFailure { error ->
            logger.warn { "Skipped unreadable gateway frame: ${error::class.simpleName}" }
        }.getOrNull()

    /** An unreadable incoming envelope is acknowledged, so the gateway stops delivering it on every reconnect. */
    private suspend fun skipUnreadable(
        encodedMessage: String,
        onUnreadableEnvelope: suspend (String) -> Unit
    ) {
        val frame = runCatching { json.parseToJsonElement(encodedMessage).jsonObject }.getOrNull() ?: return
        if ((frame["type"] as? JsonPrimitive)?.content != INCOMING_ENVELOPE_TYPE) return
        (frame["envelope"] as? JsonObject)?.envelopeIdOrNull()?.let { envelopeId -> onUnreadableEnvelope(envelopeId) }
    }

    private suspend fun handleErrorMessage(
        message: GatewayServerMessage.Error,
        onRouteRejected: (Throwable) -> Unit
    ) {
        val refusedEnvelopeId = message.envelopeId
        if (refusedEnvelopeId != null) {
            // A refused envelope is one failed send; the outbox retries or reports it.
            logger.warn { "Gateway refused envelope $refusedEnvelopeId: ${message.code}" }
            pendingRequestRegistry.rejectEnvelopeAcceptance(
                refusedEnvelopeId,
                IllegalStateException("${message.code}: ${message.message}")
            )
            return
        }
        if (message.code == "INVALID_ROUTE_REFRESH" || message.code == "ROUTE_REJECTED") {
            // Presence housekeeping can fail while message delivery still
            // works. Reconnect/retry internally; no global error snackbar.
            logger.debug { "Gateway presence response ${message.code}: ${message.message}" }
        } else {
            logger.error { "Gateway error ${message.code}: ${message.message}" }
        }
        handleGatewayError(message, onRouteRejected)
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

    private companion object {
        const val INCOMING_ENVELOPE_TYPE = "incoming_envelope"
    }
}
