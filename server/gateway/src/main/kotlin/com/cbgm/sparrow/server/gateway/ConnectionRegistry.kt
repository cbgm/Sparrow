package com.cbgm.sparrow.server.gateway

import com.cbgm.sparrow.server.protocol.GatewayServerMessage
import com.cbgm.sparrow.server.protocol.serverJson
import com.cbgm.sparrow.server.security.ClientRoutingIds
import io.ktor.server.websocket.DefaultWebSocketServerSession
import io.ktor.websocket.Frame
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap

/**
 * The gateway never learns a connection's phone aliases from its route registration (they are sealed for the
 * Control Plane). [senderAliases] holds only aliases the client sent as and a Control Plane confirmed.
 */
class GatewayConnection(
    val routingId: String,
    val connectionId: String,
    private val session: DefaultWebSocketServerSession
) {
    private val sendMutex = Mutex()
    private val senderAliases = ConcurrentHashMap.newKeySet<String>()

    /** True for the connection's routing ID and for phone aliases a Control Plane routes to it. */
    suspend fun acceptsSenderRoutingId(
        senderRoutingId: String,
        aliasOwner: suspend (alias: String) -> String?
    ): Boolean =
        when {
            senderRoutingId == routingId || senderRoutingId in senderAliases -> true
            !ClientRoutingIds.isBootstrapRoutingId(senderRoutingId) -> false
            else -> confirmSenderAlias(senderRoutingId, aliasOwner)
        }

    private suspend fun confirmSenderAlias(
        alias: String,
        aliasOwner: suspend (alias: String) -> String?
    ): Boolean {
        val owned = aliasOwner(alias) == routingId
        if (owned) senderAliases.add(alias)
        return owned
    }

    fun routingIds(): Set<String> = setOf(routingId)

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
