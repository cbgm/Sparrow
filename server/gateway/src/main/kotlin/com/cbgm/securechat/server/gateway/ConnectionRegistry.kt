package com.cbgm.securechat.server.gateway

import com.cbgm.securechat.server.protocol.GatewayServerMessage
import com.cbgm.securechat.server.protocol.serverJson
import io.ktor.server.websocket.DefaultWebSocketServerSession
import io.ktor.websocket.Frame
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap

class GatewayConnection(
    val routingId: String,
    val connectionId: String,
    routingAliases: Set<String> = emptySet(),
    private val session: DefaultWebSocketServerSession
) {
    private val sendMutex = Mutex()

    @Volatile
    private var acceptedRoutingAliases: Set<String> = routingAliases

    fun acceptsSenderRoutingId(senderRoutingId: String): Boolean =
        senderRoutingId == routingId || senderRoutingId in acceptedRoutingAliases

    fun routingIds(): Set<String> = acceptedRoutingAliases + routingId

    fun updateRoutingAliases(routingAliases: Collection<String>) {
        acceptedRoutingAliases = routingAliases.toSet()
    }

    suspend fun send(message: GatewayServerMessage) {
        sendMutex.withLock {
            session.send(Frame.Text(serverJson.encodeToString(message)))
        }
    }
}

class ConnectionRegistry {
    private val connections =
        ConcurrentHashMap<String, ConcurrentHashMap<String, GatewayConnection>>()

    fun register(connection: GatewayConnection) {
        connection.routingIds().forEach { routingId ->
            connections
                .computeIfAbsent(routingId) { ConcurrentHashMap() }[connection.connectionId] =
                connection
        }
    }

    fun updateRoutingAliases(
        connection: GatewayConnection,
        routingAliases: Collection<String>
    ) {
        remove(connection)
        connection.updateRoutingAliases(routingAliases)
        register(connection)
    }

    fun remove(connection: GatewayConnection) {
        connections.entries.forEach { entry ->
            val routes = entry.value
            routes.remove(connection.connectionId, connection)
            if (routes.isEmpty()) {
                connections.remove(entry.key, routes)
            }
        }
    }

    fun find(routingId: String): List<GatewayConnection> =
        connections[routingId]
            ?.values
            ?.distinctBy(GatewayConnection::connectionId)
            .orEmpty()

    fun resolveCanonicalRoutingId(routingId: String): String? =
        find(routingId).firstOrNull()?.routingId

    fun count(): Int =
        connections
            .values
            .flatMap { routes -> routes.values }
            .distinctBy(GatewayConnection::connectionId)
            .size
}
