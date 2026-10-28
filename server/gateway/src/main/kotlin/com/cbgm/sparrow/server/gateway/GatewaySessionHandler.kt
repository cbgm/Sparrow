package com.cbgm.sparrow.server.gateway

import com.cbgm.sparrow.server.protocol.ClientRouteRegistration
import com.cbgm.sparrow.server.protocol.GatewayClientMessage
import com.cbgm.sparrow.server.protocol.GatewayServerMessage
import com.cbgm.sparrow.server.protocol.serverJson
import io.ktor.server.websocket.DefaultWebSocketServerSession
import io.ktor.websocket.CloseReason
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.security.SecureRandom
import kotlin.time.Duration.Companion.milliseconds

internal class GatewaySessionHandler(
    private val nodeId: String,
    private val connections: ConnectionRegistry,
    private val presence: PresenceClient,
    private val pushActions: GatewayPushActions,
    private val routeValidator: GatewayRouteValidator,
    private val signInTimeoutMilliseconds: Long,
    private val actions: GatewayMessageActions
) {
    suspend fun handle(session: DefaultWebSocketServerSession) {
        val nonce = newChallengeNonce()
        val state = GatewaySessionState(challengeNonce = nonce)
        val workDispatcher = GatewaySessionWorkDispatcher()
        val signInDeadline = session.launch { closeIfNotSignedIn(session, state) }
        try {
            session.sendServerMessage(GatewayServerMessage.Challenge(nonce = nonce, nodeId = nodeId))
            session.incoming.consumeEach { frame ->
                handleFrame(
                    session = session,
                    state = state,
                    frame = frame,
                    workDispatcher = workDispatcher
                )
            }
        } finally {
            signInDeadline.cancel()
            workDispatcher.close()
            cleanup(state.connection)
        }
    }

    // An unauthenticated socket would otherwise hold a connection slot until the transport timeout.
    private suspend fun closeIfNotSignedIn(
        session: DefaultWebSocketServerSession,
        state: GatewaySessionState
    ) {
        delay(signInTimeoutMilliseconds.milliseconds)
        if (state.connection == null) {
            state.challengeNonce = null
            session.rejectSession("Sign-in timed out")
        }
    }

    private suspend fun handleFrame(
        session: DefaultWebSocketServerSession,
        state: GatewaySessionState,
        frame: Frame,
        workDispatcher: GatewaySessionWorkDispatcher
    ) {
        if (frame is Frame.Text) {
            decodeMessage(session, state, frame)?.let { message ->
                handleMessage(
                    session = session,
                    state = state,
                    message = message,
                    workDispatcher = workDispatcher
                )
            }
        } else {
            rejectUnsupportedFrame(session, state)
        }
    }

    private suspend fun rejectUnsupportedFrame(
        session: DefaultWebSocketServerSession,
        state: GatewaySessionState
    ) {
        val connection = state.connection
        if (connection == null) {
            rejectBeforeSignIn(session, state)
        } else {
            connection.sendError(
                code = "UNSUPPORTED_FRAME",
                message = "Text frames are required"
            )
        }
    }

    private suspend fun decodeMessage(
        session: DefaultWebSocketServerSession,
        state: GatewaySessionState,
        frame: Frame.Text
    ): GatewayClientMessage? =
        runCatching {
            serverJson.decodeFromString<GatewayClientMessage>(frame.readText())
        }.getOrElse {
            rejectInvalidMessage(session, state)
            null
        }

    // Before sign-in an undecodable frame is most likely a Register without proof, so the session
    // is closed instead of being left open unauthenticated.
    private suspend fun rejectInvalidMessage(
        session: DefaultWebSocketServerSession,
        state: GatewaySessionState
    ) {
        if (state.connection == null) {
            rejectBeforeSignIn(session, state)
        } else {
            session.sendError(
                code = "INVALID_MESSAGE",
                message = "Invalid message"
            )
        }
    }

    // Nothing but Register is accepted before sign-in, so an unauthenticated socket can never
    // send, acknowledge, refresh a route or obtain an upload ticket.
    private suspend fun handleMessage(
        session: DefaultWebSocketServerSession,
        state: GatewaySessionState,
        message: GatewayClientMessage,
        workDispatcher: GatewaySessionWorkDispatcher
    ) {
        val connection = state.connection
        when {
            message is GatewayClientMessage.Register -> handleRegistration(session, state, message)
            connection == null -> rejectBeforeSignIn(session, state)
            else -> handleSignedInMessage(connection, message, workDispatcher)
        }
    }

    private suspend fun rejectBeforeSignIn(
        session: DefaultWebSocketServerSession,
        state: GatewaySessionState
    ) {
        state.challengeNonce = null
        session.rejectSession("Register with a session proof first")
    }

    private suspend fun handleSignedInMessage(
        connection: GatewayConnection,
        message: GatewayClientMessage,
        workDispatcher: GatewaySessionWorkDispatcher
    ) {
        when (message) {
            is GatewayClientMessage.Register -> Unit
            is GatewayClientMessage.SendEnvelope ->
                workDispatcher.dispatch(key = "envelope:${message.envelope.recipientId}") {
                    actions.sendEnvelope(connection, message)
                }
            is GatewayClientMessage.SendFederatedEnvelope ->
                workDispatcher.dispatch(key = "envelope:${message.envelope.recipientDeviceRoutingId}") {
                    actions.sendFederatedEnvelope(connection, message)
                }
            is GatewayClientMessage.IndicatorState ->
                workDispatcher.dispatch(key = "indicator:${message.recipientId}") {
                    actions.deliverIndicator(connection, message)
                }
            is GatewayClientMessage.AcknowledgeEnvelope ->
                pushActions.acknowledge(connection, message.envelopeId)
            is GatewayClientMessage.RefreshRoute -> refreshRoute(connection, message.registration)
            is GatewayClientMessage.RequestBlobUploadTicket ->
                actions.issueBlobUploadTicket(connection, message)
        }
    }

    private suspend fun handleRegistration(
        session: DefaultWebSocketServerSession,
        state: GatewaySessionState,
        message: GatewayClientMessage.Register
    ) {
        val current = state.connection
        if (current == null) {
            state.connection = register(session, state, message)
        } else {
            current.sendError(
                code = "ALREADY_REGISTERED",
                message = "Already registered"
            )
        }
    }

    // The challenge nonce is consumed by the first attempt, so a failed proof cannot be retried and
    // a signature can never be used twice.
    private suspend fun register(
        session: DefaultWebSocketServerSession,
        state: GatewaySessionState,
        message: GatewayClientMessage.Register
    ): GatewayConnection? {
        val nonce = state.challengeNonce.also { state.challengeNonce = null }
        val failure =
            GatewaySessionValidator.validationFailure(
                message = message,
                nonce = nonce,
                expectedNodeId = nodeId
            )
        if (failure != null) {
            session.rejectSession("Session proof is invalid: ${failure.name}")
            return null
        }

        val connection = message.toGatewayConnection(session)
        activateConnection(connection)
        return connection
    }

    private suspend fun activateConnection(connection: GatewayConnection) {
        connections.register(connection)
        connection.send(
            GatewayServerMessage.Registered(
                routingId = connection.routingId
            )
        )
        pushActions.deliverPending(connection)
    }

    private suspend fun refreshRoute(
        connection: GatewayConnection,
        registration: ClientRouteRegistration
    ) {
        val routeValidationFailure =
            routeValidator.validationFailure(
                registration = registration,
                connectionRoutingId = connection.routingId,
                connectionId = connection.connectionId,
                expectedNodeId = nodeId
            )

        if (routeValidationFailure != null) {
            connection.sendError(
                code = "INVALID_ROUTE_REFRESH",
                message = "Signed route is invalid: ${routeValidationFailure.name}"
            )
            return
        }

        when (val presenceResult = synchronizePresence(registration)) {
            PresenceRegistrationResult.ACCEPTED -> {
                val aliases = registration.route.aliases.orEmpty()
                connections.updateRoutingAliases(
                    connection = connection,
                    routingAliases = aliases
                )
                connection.send(
                    GatewayServerMessage.RouteRegistered(aliases = aliases)
                )
                pushActions.deliverPending(connection)
            }

            else -> {
                connections.updateRoutingAliases(connection, emptyList())
                connection.sendRouteRejected(presenceResult)
            }
        }
    }

    private suspend fun synchronizePresence(
        registration: ClientRouteRegistration
    ): PresenceRegistrationResult =
        synchronizePresenceRegistration(
            presence = presence,
            registration = registration
        )

    private suspend fun cleanup(connection: GatewayConnection?) {
        connection?.let { current ->
            connections.remove(current)
            runCatching {
                presence.remove(
                    routingId = current.routingId,
                    connectionId = current.connectionId
                )
            }
        }
    }
}

internal suspend fun synchronizePresenceRegistration(
    presence: PresenceClient,
    registration: ClientRouteRegistration
): PresenceRegistrationResult =
    try {
        presence.register(registration)
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (_: Exception) {
        PresenceRegistrationResult.REJECTED
    }

private suspend fun GatewayConnection.sendRouteRejected(result: PresenceRegistrationResult) {
    sendError(
        code = "ROUTE_REJECTED",
        message =
            if (result == PresenceRegistrationResult.PHONE_VERIFICATION_FAILED) {
                "Phone verification failed"
            } else {
                "Presence route rejected"
            }
    )
}

internal data class GatewayPushActions(
    val deliverPending: (GatewayConnection) -> Unit,
    val acknowledge: (GatewayConnection, String) -> Unit
)

internal data class GatewayMessageActions(
    val sendEnvelope: suspend (GatewayConnection, GatewayClientMessage.SendEnvelope) -> Unit,
    val sendFederatedEnvelope:
        suspend (GatewayConnection, GatewayClientMessage.SendFederatedEnvelope) -> Unit,
    val deliverIndicator: suspend (GatewayConnection, GatewayClientMessage.IndicatorState) -> Unit,
    val issueBlobUploadTicket:
        suspend (GatewayConnection, GatewayClientMessage.RequestBlobUploadTicket) -> Unit
)

// Read by the sign-in deadline coroutine as well as the frame loop.
private class GatewaySessionState(
    @Volatile var challengeNonce: ByteArray?,
    @Volatile var connection: GatewayConnection? = null
)

private val challengeRandom = SecureRandom()

private fun newChallengeNonce(): ByteArray =
    ByteArray(CHALLENGE_NONCE_BYTES).also(challengeRandom::nextBytes)

private const val CHALLENGE_NONCE_BYTES = 32

private fun GatewayClientMessage.Register.toGatewayConnection(
    session: DefaultWebSocketServerSession
): GatewayConnection =
    GatewayConnection(
        routingId = routingId,
        connectionId = connectionId,
        session = session
    )

private suspend fun DefaultWebSocketServerSession.sendError(
    code: String,
    message: String
) {
    sendServerMessage(
        GatewayServerMessage.Error(
            code = code,
            message = message
        )
    )
}

private suspend fun DefaultWebSocketServerSession.sendServerMessage(message: GatewayServerMessage) {
    send(Frame.Text(serverJson.encodeToString<GatewayServerMessage>(message)))
}

// The peer may already be gone; closing must not fail the session handler.
private suspend fun DefaultWebSocketServerSession.rejectSession(reason: String) {
    runCatching {
        sendError(code = "INVALID_SESSION", message = reason)
        close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "Invalid session"))
    }
}

private suspend fun GatewayConnection.sendError(
    code: String,
    message: String
) {
    send(
        GatewayServerMessage.Error(
            code = code,
            message = message
        )
    )
}
