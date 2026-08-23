package com.cbgm.sparrow.server.gateway

import com.cbgm.sparrow.server.protocol.ClientRoute
import com.cbgm.sparrow.server.protocol.ClientRouteRegistration
import com.cbgm.sparrow.server.protocol.GatewayClientMessage
import com.cbgm.sparrow.server.protocol.GatewayServerMessage
import com.cbgm.sparrow.server.protocol.serverJson
import io.ktor.server.websocket.DefaultWebSocketServerSession
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.channels.consumeEach
import java.util.UUID

internal class GatewaySessionHandler(
    private val nodeId: String,
    private val connections: ConnectionRegistry,
    private val presence: PresenceClient,
    private val pushActions: GatewayPushActions,
    private val routeValidator: GatewayRouteValidator,
    private val actions: GatewayMessageActions
) {
    suspend fun handle(session: DefaultWebSocketServerSession) {
        val state = GatewaySessionState()
        val workDispatcher = GatewaySessionWorkDispatcher()
        try {
            session.incoming.consumeEach { frame ->
                handleFrame(
                    session = session,
                    state = state,
                    frame = frame,
                    workDispatcher = workDispatcher
                )
            }
        } finally {
            workDispatcher.close()
            cleanup(state.connection)
        }
    }

    private suspend fun handleFrame(
        session: DefaultWebSocketServerSession,
        state: GatewaySessionState,
        frame: Frame,
        workDispatcher: GatewaySessionWorkDispatcher
    ) {
        if (frame is Frame.Text) {
            decodeMessage(session, frame)?.let { message ->
                handleMessage(
                    session = session,
                    state = state,
                    message = message,
                    workDispatcher = workDispatcher
                )
            }
        } else {
            state.connection?.sendError(
                code = "UNSUPPORTED_FRAME",
                message = "Text frames are required"
            )
        }
    }

    private suspend fun decodeMessage(
        session: DefaultWebSocketServerSession,
        frame: Frame.Text
    ): GatewayClientMessage? =
        runCatching {
            serverJson.decodeFromString<GatewayClientMessage>(frame.readText())
        }.getOrElse {
            session.sendError(
                code = "INVALID_MESSAGE",
                message = "Invalid message"
            )
            null
        }

    private suspend fun handleMessage(
        session: DefaultWebSocketServerSession,
        state: GatewaySessionState,
        message: GatewayClientMessage,
        workDispatcher: GatewaySessionWorkDispatcher
    ) {
        when (message) {
            is GatewayClientMessage.Register -> handleRegistration(session, state, message)
            is GatewayClientMessage.SendEnvelope ->
                state.connection?.let { connection ->
                    workDispatcher.dispatch(
                        key = "envelope:${message.envelope.recipientId}"
                    ) {
                        actions.sendEnvelope(connection, message)
                    }
                } ?: session.sendNotRegistered()

            is GatewayClientMessage.SendFederatedEnvelope ->
                state.connection?.let { connection ->
                    workDispatcher.dispatch(
                        key = "envelope:${message.envelope.recipientDeviceRoutingId}"
                    ) {
                        actions.sendFederatedEnvelope(connection, message)
                    }
                } ?: session.sendNotRegistered()

            is GatewayClientMessage.TypingState ->
                state.connection?.let { connection ->
                    workDispatcher.dispatch(
                        key = "typing:${message.recipientId}"
                    ) {
                        actions.deliverTyping(connection, message)
                    }
                }

            is GatewayClientMessage.AcknowledgeEnvelope ->
                state.connection?.let { connection ->
                    pushActions.acknowledge(connection, message.envelopeId)
                }

            is GatewayClientMessage.RefreshRoute ->
                refreshRoute(state.connection, message.registration)

            is GatewayClientMessage.RequestBlobUploadTicket ->
                state.connection?.let { connection ->
                    actions.issueBlobUploadTicket(connection, message)
                } ?: session.sendNotRegistered()
        }
    }

    private suspend fun handleRegistration(
        session: DefaultWebSocketServerSession,
        state: GatewaySessionState,
        message: GatewayClientMessage.Register
    ) {
        val current = state.connection
        if (current == null) {
            state.connection = register(session, message)
        } else {
            current.sendError(
                code = "ALREADY_REGISTERED",
                message = "Already registered"
            )
        }
    }

    private suspend fun register(
        session: DefaultWebSocketServerSession,
        message: GatewayClientMessage.Register
    ): GatewayConnection? {
        val connection = message.toConnection(session)
        val registration = message.toRouteRegistration(connection, nodeId)
        val routeValidationFailure =
            registration?.let { currentRegistration ->
                routeValidator.validationFailure(
                    registration = currentRegistration,
                    connectionRoutingId = connection.routingId,
                    connectionId = connection.connectionId,
                    expectedNodeId = nodeId
                )
            }

        if (routeValidationFailure != null) {
            connection.sendError(
                code = "INVALID_ROUTE",
                message = "Signed route is invalid: ${routeValidationFailure.name}"
            )
            return null
        }

        val routeAccepted = registration?.let { synchronizePresence(it) } ?: true
        return if (routeAccepted) {
            activateConnection(connection)
            connection
        } else {
            connection.sendError(
                code = "ROUTE_REJECTED",
                message = "Presence route rejected"
            )
            null
        }
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
        connection: GatewayConnection?,
        registration: ClientRouteRegistration
    ) {
        if (connection == null) {
            return
        }

        val routeValidationFailure =
            routeValidator.validationFailure(
                registration = registration,
                connectionRoutingId = connection.routingId,
                connectionId = connection.connectionId,
                expectedNodeId = nodeId
            )

        when {
            routeValidationFailure != null ->
                connection.sendError(
                    code = "INVALID_ROUTE_REFRESH",
                    message = "Signed route is invalid: ${routeValidationFailure.name}"
                )

            !synchronizePresence(registration) ->
                connection.sendError(
                    code = "ROUTE_REJECTED",
                    message = "Presence route rejected"
                )

            else -> {
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
        }
    }

    private suspend fun synchronizePresence(registration: ClientRouteRegistration): Boolean =
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
): Boolean =
    runCatching {
        presence.register(registration)
    }.getOrDefault(false)

internal data class GatewayPushActions(
    val deliverPending: (GatewayConnection) -> Unit,
    val acknowledge: (GatewayConnection, String) -> Unit
)

internal data class GatewayMessageActions(
    val sendEnvelope: suspend (GatewayConnection, GatewayClientMessage.SendEnvelope) -> Unit,
    val sendFederatedEnvelope:
        suspend (GatewayConnection, GatewayClientMessage.SendFederatedEnvelope) -> Unit,
    val deliverTyping: suspend (GatewayConnection, GatewayClientMessage.TypingState) -> Unit,
    val issueBlobUploadTicket:
        suspend (GatewayConnection, GatewayClientMessage.RequestBlobUploadTicket) -> Unit
)

private data class GatewaySessionState(
    var connection: GatewayConnection? = null
)

private fun GatewayClientMessage.Register.toConnection(
    session: DefaultWebSocketServerSession
): GatewayConnection =
    GatewayConnection(
        routingId = routingId,
        connectionId = connectionId ?: UUID.randomUUID().toString(),
        routingAliases = aliases?.takeIf { generation != null }.orEmpty().toSet(),
        session = session
    )

private fun GatewayClientMessage.Register.toRouteRegistration(
    connection: GatewayConnection,
    nodeId: String
): ClientRouteRegistration? {
    val routeGeneration = generation
    val routeExpiration = expiresAtEpochMilliseconds
    val signingPublicKey = clientSigningPublicKey
    val routeSignature = clientSignature

    return when {
        routeGeneration == null -> null
        routeExpiration == null -> null
        signingPublicKey == null -> null
        routeSignature == null -> null
        else ->
            ClientRouteRegistration(
                route =
                    ClientRoute(
                        routingId = routingId,
                        nodeId = nodeId,
                        connectionId = connection.connectionId,
                        generation = routeGeneration,
                        expiresAtEpochMilliseconds = routeExpiration,
                        aliases = aliases,
                        clientSignature = routeSignature
                    ),
                clientSigningPublicKey = signingPublicKey
            )
    }
}

private suspend fun DefaultWebSocketServerSession.sendNotRegistered() {
    sendError(
        code = "NOT_REGISTERED",
        message = "Register first"
    )
}

private suspend fun DefaultWebSocketServerSession.sendError(
    code: String,
    message: String
) {
    send(
        Frame.Text(
            serverJson.encodeToString<GatewayServerMessage>(
                GatewayServerMessage.Error(
                    code = code,
                    message = message
                )
            )
        )
    )
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
