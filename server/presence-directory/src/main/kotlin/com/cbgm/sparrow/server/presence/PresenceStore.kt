package com.cbgm.sparrow.server.presence

import com.cbgm.sparrow.server.protocol.ClientRoute
import com.cbgm.sparrow.server.protocol.ClientRouteRegistration
import com.cbgm.sparrow.server.protocol.ClientRoutingResult
import com.cbgm.sparrow.server.protocol.InvitationRoute
import com.cbgm.sparrow.server.protocol.InvitationRouteResult
import com.cbgm.sparrow.server.security.ClientRoutingIds
import com.cbgm.sparrow.server.security.ProtocolSignatures
import java.util.concurrent.ConcurrentHashMap

private const val DEFAULT_MAXIMUM_TTL_MILLISECONDS = 120_000L

/**
 * Routes are stored under the device routing ID only. Phone aliases are separate bindings (alias → device) that the
 * app claims directly ([bindAliases]); resolving an alias resolves the device holding it.
 */
interface PresenceStorage : AutoCloseable {
    val persistenceMode: String

    suspend fun register(registration: ClientRouteRegistration): PresenceResult

    /** Binds the claimed aliases [claims] allows to [deviceRoutingId] and returns those it bound. */
    suspend fun bindAliases(
        deviceRoutingId: String,
        claims: AliasClaims
    ): List<String>

    /**
     * Keeps the device's invitation route from its latest claim, as long as an alias binding; null removes it. The
     * claim is signed with the device key, so only the device itself sets it.
     */
    suspend fun saveInvitationRoute(
        deviceRoutingId: String,
        invitationRoute: InvitationRoute?
    )

    /**
     * The invitation route for a phone alias (of the device holding it) or a device routing ID, or null when there is
     * none that is still valid.
     */
    suspend fun invitationRoute(routingId: String): InvitationRouteResult?

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
    private val invitationRoutes = ConcurrentHashMap<String, KeptInvitationRoute>()
    private val lock = Any()
    override val persistenceMode: String = "memory"

    override suspend fun register(registration: ClientRouteRegistration): PresenceResult =
        synchronized(lock) {
            val currentTime = now()
            validatePresenceRegistration(registration, maximumTtlMilliseconds, currentTime)
                ?: registerValidated(registration.route.copy(aliases = null))
        }

    private fun registerValidated(route: ClientRoute): PresenceResult {
        purgeExpired()
        val deviceRoutes = routes.computeIfAbsent(route.routingId) { ConcurrentHashMap() }
        val newestGeneration = deviceRoutes.values.maxOfOrNull(ClientRoute::generation) ?: -1L
        return when {
            route.generation < newestGeneration -> PresenceResult.Rejected("STALE_GENERATION")
            else -> {
                if (route.generation > newestGeneration) deviceRoutes.clear()
                deviceRoutes[route.connectionId] = route
                PresenceResult.Accepted
            }
        }
    }

    override suspend fun bindAliases(
        deviceRoutingId: String,
        claims: AliasClaims
    ): List<String> =
        synchronized(lock) {
            val currentTime = now()
            aliasOwners.entries.removeIf { (_, binding) -> binding.expiresAtEpochMilliseconds <= currentTime }
            claims.aliases
                .filter { alias -> claims.allows(alias, deviceRoutingId, aliasOwners[alias]?.owner) }
                .onEach { alias ->
                    aliasOwners[alias] = AliasBinding(deviceRoutingId, currentTime + ALIAS_BINDING_LIFETIME_MILLISECONDS)
                }
        }

    override suspend fun saveInvitationRoute(
        deviceRoutingId: String,
        invitationRoute: InvitationRoute?
    ) {
        val currentTime = now()
        invitationRoutes.entries.removeIf { (_, kept) -> kept.expiresAtEpochMilliseconds <= currentTime }
        when (invitationRoute) {
            null -> invitationRoutes.remove(deviceRoutingId)
            else -> invitationRoutes[deviceRoutingId] = KeptInvitationRoute(invitationRoute, currentTime + ALIAS_BINDING_LIFETIME_MILLISECONDS)
        }
    }

    override suspend fun invitationRoute(routingId: String): InvitationRouteResult? {
        val currentTime = now()
        val device = aliasOwners[routingId]?.takeIf { it.expiresAtEpochMilliseconds > currentTime }?.owner ?: routingId
        return invitationRoutes[device]
            ?.takeIf { kept -> kept.expiresAtEpochMilliseconds > currentTime && kept.route.route.expiresAtEpochMilliseconds > currentTime }
            ?.let { kept -> InvitationRouteResult(device, kept.route) }
    }

    override suspend fun remove(routingId: String, connectionId: String) {
        synchronized(lock) {
            routes[routingId]?.let { deviceRoutes ->
                deviceRoutes.remove(connectionId)
                if (deviceRoutes.isEmpty()) routes.remove(routingId, deviceRoutes)
            }
        }
    }

    override suspend fun resolve(routingId: String): ClientRoutingResult {
        purgeExpired()
        val owner =
            aliasOwners[routingId]
                ?.takeIf { binding -> binding.expiresAtEpochMilliseconds > now() }
                ?.owner
        return ClientRoutingResult(
            routingId = routingId,
            routes = routes[owner ?: routingId]?.values?.sortedBy(ClientRoute::nodeId).orEmpty(),
            ownerRoutingId = owner
        )
    }

    override suspend fun routeCount(): Int {
        purgeExpired()
        return routes.values.sumOf { deviceRoutes -> deviceRoutes.size }
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

    private data class KeptInvitationRoute(
        val route: InvitationRoute,
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
    data object Accepted : PresenceResult

    data class Rejected(
        val code: String
    ) : PresenceResult
}
