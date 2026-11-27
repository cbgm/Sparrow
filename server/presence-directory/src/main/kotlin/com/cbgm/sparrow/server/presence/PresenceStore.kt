package com.cbgm.sparrow.server.presence

import com.cbgm.sparrow.server.protocol.InvitationRoute
import com.cbgm.sparrow.server.protocol.InvitationRouteResult
import java.util.concurrent.ConcurrentHashMap

/**
 * Which device holds a phone alias, and the invitation route of each device. Apps claim both directly ([bindAliases],
 * [saveInvitationRoute]); devices hold no routes here, since they receive only through their mailboxes.
 */
interface PresenceStorage : AutoCloseable {
    val persistenceMode: String

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

    /** The device holding [alias], or null while it is free. */
    suspend fun aliasOwner(alias: String): String?
}

class PresenceStore(
    private val now: () -> Long = System::currentTimeMillis
) : PresenceStorage {
    private val aliasOwners = ConcurrentHashMap<String, AliasBinding>()
    private val invitationRoutes = ConcurrentHashMap<String, KeptInvitationRoute>()
    private val lock = Any()
    override val persistenceMode: String = "memory"

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
        val device = aliasOwner(routingId) ?: routingId
        return invitationRoutes[device]
            ?.takeIf { kept -> kept.expiresAtEpochMilliseconds > currentTime && kept.route.route.expiresAtEpochMilliseconds > currentTime }
            ?.let { kept -> InvitationRouteResult(device, kept.route) }
    }

    override suspend fun aliasOwner(alias: String): String? =
        aliasOwners[alias]?.takeIf { binding -> binding.expiresAtEpochMilliseconds > now() }?.owner

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
