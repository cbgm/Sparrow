package com.cbgm.sparrow.server.presence

import com.cbgm.sparrow.server.protocol.ClientRoute
import com.cbgm.sparrow.server.protocol.ClientRouteRegistration
import com.cbgm.sparrow.server.protocol.ClientRoutingResult
import com.cbgm.sparrow.server.security.ClientRoutingIds
import com.cbgm.sparrow.server.security.ProtocolSignatures
import java.util.concurrent.ConcurrentHashMap

private const val DEFAULT_MAXIMUM_TTL_MILLISECONDS = 120_000L

interface PresenceStorage : AutoCloseable {
    val persistenceMode: String

    suspend fun register(
        registration: ClientRouteRegistration,
        claims: AliasClaims
    ): PresenceResult

    suspend fun remove(routingId: String, connectionId: String)

    suspend fun resolve(routingId: String): ClientRoutingResult

    suspend fun routeCount(): Int
}

class PresenceStore(
    private val maximumTtlMilliseconds: Long = DEFAULT_MAXIMUM_TTL_MILLISECONDS,
    private val now: () -> Long = System::currentTimeMillis
) : PresenceStorage {
    private val routes = ConcurrentHashMap<String, ConcurrentHashMap<String, ClientRoute>>()
    private val aliasOwners = ConcurrentHashMap<String, AliasBinding>()
    private val lock = Any()
    override val persistenceMode: String = "memory"

    override suspend fun register(
        registration: ClientRouteRegistration,
        claims: AliasClaims
    ): PresenceResult =
        synchronized(lock) {
            val route = registration.route
            val currentTime = now()
            validatePresenceRegistration(
                registration,
                maximumTtlMilliseconds,
                currentTime
            ) ?: registerValidated(route, claims, currentTime)
        }

    private fun registerValidated(
        route: ClientRoute,
        claims: AliasClaims,
        currentTime: Long
    ): PresenceResult {
        purgeExpired()
        aliasOwners.entries.removeIf { (_, binding) -> binding.expiresAtEpochMilliseconds <= currentTime }
        val aliases =
            route.aliases.orEmpty().filter { alias ->
                claims.allows(alias, route.routingId, aliasOwners[alias]?.owner)
            }
        val routingIds = listOf(route.routingId) + aliases
        val newestGeneration =
            routingIds
                .filter { routingId -> routingId == route.routingId || aliasOwners[routingId]?.owner == route.routingId }
                .mapNotNull { routingId -> routes[routingId]?.values?.maxOfOrNull(ClientRoute::generation) }
                .maxOrNull()
                ?: -1L

        return when {
            route.generation < newestGeneration -> PresenceResult.Rejected("STALE_GENERATION")
            else -> {
                aliases
                    .filter { alias -> aliasOwners[alias]?.owner != route.routingId }
                    .forEach { alias -> routes.remove(alias) }
                routingIds.forEach { routingId ->
                    val deviceRoutes = routes.computeIfAbsent(routingId) { ConcurrentHashMap() }
                    if (route.generation > (
                            deviceRoutes.values.maxOfOrNull(ClientRoute::generation)
                                ?: -1L
                        )
                    ) {
                        deviceRoutes.clear()
                    }
                    deviceRoutes[route.connectionId] = route
                }
                aliases.forEach { alias ->
                    aliasOwners[alias] =
                        AliasBinding(route.routingId, currentTime + ALIAS_BINDING_LIFETIME_MILLISECONDS)
                }
                PresenceResult.Accepted(aliases)
            }
        }
    }

    override suspend fun remove(routingId: String, connectionId: String) {
        synchronized(lock) { removeRoute(routingId, connectionId) }
    }

    private fun removeRoute(routingId: String, connectionId: String) {
        val route = routes[routingId]?.get(connectionId)
        val deviceRoutingId = route?.routingId ?: routingId
        // Connection IDs are chosen by clients, so an alias only loses a route its own device stored.
        val aliases =
            route?.aliases.orEmpty().filter { alias ->
                routes[alias]?.get(connectionId)?.routingId == deviceRoutingId
            }
        (listOf(deviceRoutingId) + aliases).distinct().forEach { currentRoutingId ->
            routes[currentRoutingId]?.let { deviceRoutes ->
                deviceRoutes.remove(connectionId)
                if (deviceRoutes.isEmpty()) routes.remove(currentRoutingId, deviceRoutes)
            }
        }
    }

    override suspend fun resolve(routingId: String): ClientRoutingResult {
        purgeExpired()
        return ClientRoutingResult(
            routingId = routingId,
            routes = routes[routingId]?.values?.sortedBy(ClientRoute::nodeId).orEmpty()
        )
    }

    override suspend fun routeCount(): Int {
        purgeExpired()
        return routes
            .filterKeys(ClientRoutingIds::isDeviceRoutingId)
            .values
            .sumOf { deviceRoutes -> deviceRoutes.size }
    }

    private fun purgeExpired() {
        val currentTime = now()
        routes.forEach { (routingId, deviceRoutes) ->
            deviceRoutes.entries.removeIf { (_, route) -> route.expiresAtEpochMilliseconds <= currentTime }
            if (deviceRoutes.isEmpty()) routes.remove(routingId, deviceRoutes)
        }
    }

    override fun close() = Unit

    private data class AliasBinding(
        val owner: String,
        val expiresAtEpochMilliseconds: Long
    )
}

internal fun validatePresenceRegistration(
    registration: ClientRouteRegistration,
    maximumTtlMilliseconds: Long,
    currentTime: Long
): PresenceResult.Rejected? {
    val route = registration.route
    return when {
        !ClientRoutingIds.matchesSigningPublicKey(
            route.routingId,
            registration.clientSigningPublicKey
        ) ->
            PresenceResult.Rejected("INVALID_ROUTING_ID")

        route.aliases.orEmpty().any { alias -> !ClientRoutingIds.isBootstrapRoutingId(alias) } ->
            PresenceResult.Rejected("INVALID_ROUTING_ALIAS")

        route.aliases.orEmpty().distinct().size != route.aliases.orEmpty().size ->
            PresenceResult.Rejected("DUPLICATE_ROUTING_ALIAS")

        route.expiresAtEpochMilliseconds <= currentTime -> PresenceResult.Rejected("ROUTE_EXPIRED")
        route.expiresAtEpochMilliseconds - currentTime > maximumTtlMilliseconds ->
            PresenceResult.Rejected(
                "TTL_TOO_LONG"
            )

        !ProtocolSignatures.verifyClientRoute(route, registration.clientSigningPublicKey) ->
            PresenceResult.Rejected("INVALID_SIGNATURE")

        else -> null
    }
}

sealed interface PresenceResult {
    /** [aliases] are the requested aliases the route was stored under; the others were refused. */
    data class Accepted(
        val aliases: List<String>
    ) : PresenceResult

    data class Rejected(
        val code: String
    ) : PresenceResult
}
