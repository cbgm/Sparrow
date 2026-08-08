package com.cbgm.securechat.server.gateway

import com.cbgm.securechat.server.protocol.ClientRoute
import com.cbgm.securechat.server.protocol.ClientRouteRegistration
import com.cbgm.securechat.server.protocol.GatewayClientMessage
import com.cbgm.securechat.server.protocol.GatewayServerMessage
import com.cbgm.securechat.server.protocol.serverJson
import io.ktor.server.websocket.DefaultWebSocketServerSession
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.channels.consumeEach
import java.util.UUID

internal class GatewaySessionHandler(
    private val nodeId: String,
    private val connections: ConnectionRegistry,
    private val presence: PresenceClient,
    private val legacyPush: LegacyPushClient,
    private val routeValidator: GatewayRouteValidator,
    private val actions: GatewayMessageActions
) {
    suspend fun handle(session: DefaultWebSocketServerSession) {
        val state = GatewaySessionState()
        try {
            session.incoming.consumeEach { frame ->
                handleFrame(session, state, frame)
            }
        } finally {
            cleanup(state.connection)
        }
    }

    private suspend fun handleFrame(
        session: DefaultWebSocketServerSession,
        state: GatewaySessionState,
        frame: Frame
    ) {
        if (frame is Frame.Text) {
            decodeMessage(session, frame)?.let { message ->
                handleMessage(session, state, message)
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
        message: GatewayClientMessage
    ) {
        when (message) {
            is GatewayClientMessage.Register -> handleRegistration(session, state, message)
            is GatewayClientMessage.SendEnvelope ->
                state.connection?.let { connection ->
                    actions.sendEnvelope(connection, message)
                } ?: session.sendNotRegistered()

            is GatewayClientMessage.SendFederatedEnvelope ->
                state.connection?.let { connection ->
                    actions.sendFederatedEnvelope(connection, message)
                } ?: session.sendNotRegistered()

            is GatewayClientMessage.TypingState ->
                state.connection?.let { connection ->
                    actions.deliverTyping(connection, message)
                }

            is GatewayClientMessage.AcknowledgeEnvelope ->
                state.connection?.let { connection ->
                    runCatching {
                        legacyPush.acknowledge(
                            recipientId = connection.routingId,
                            envelopeId = message.envelopeId
                        )
                    }
                }

            is GatewayClientMessage.RefreshRoute ->
                refreshRoute(state.connection, message.registration)
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
        val routeIsValid =
            registration?.let { currentRegistration ->
                routeValidator.isValid(
                    registration = currentRegistration,
                    connectionRoutingId = connection.routingId,
                    connectionId = connection.connectionId,
                    expectedNodeId = nodeId
                )
            } ?: true

        if (!routeIsValid) {
            connection.sendError(
                code = "INVALID_ROUTE",
                message = "Signed route is invalid"
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
                relayId = connection.routingId
            )
        )
        runCatching {
            legacyPush.pending(recipientId = connection.routingId)
        }.getOrDefault(emptyList()).forEach { envelope ->
            connection.send(
                GatewayServerMessage.IncomingEnvelope(
                    envelope = envelope
                )
            )
        }
    }

    private suspend fun refreshRoute(
        connection: GatewayConnection?,
        registration: ClientRouteRegistration
    ) {
        if (connection == null) {
            return
        }

        val routeIsValid =
            routeValidator.isValid(
                registration = registration,
                connectionRoutingId = connection.routingId,
                connectionId = connection.connectionId,
                expectedNodeId = nodeId
            )

        when {
            !routeIsValid ->
                connection.sendError(
                    code = "INVALID_ROUTE_REFRESH",
                    message = "Signed route is invalid"
                )

            !synchronizePresence(registration) ->
                connection.sendError(
                    code = "ROUTE_REJECTED",
                    message = "Presence route rejected"
                )

            else ->
                connections.updateRoutingAliases(
                    connection = connection,
                    routingAliases = registration.route.aliases.orEmpty()
                )
        }
    }

    private suspend fun synchronizePresence(registration: ClientRouteRegistration): Boolean =
        runCatching {
            presence.register(registration)
        }.getOrElse {
            true
        }

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

internal data class GatewayMessageActions(
    val sendEnvelope: suspend (GatewayConnection, GatewayClientMessage.SendEnvelope) -> Unit,
    val sendFederatedEnvelope:
        suspend (GatewayConnection, GatewayClientMessage.SendFederatedEnvelope) -> Unit,
    val deliverTyping: suspend (GatewayConnection, GatewayClientMessage.TypingState) -> Unit
)

private data class GatewaySessionState(
    var connection: GatewayConnection? = null
)

private fun GatewayClientMessage.Register.toConnection(
    session: DefaultWebSocketServerSession
): GatewayConnection =
    GatewayConnection(
        routingId = relayId,
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
                        routingId = relayId,
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
