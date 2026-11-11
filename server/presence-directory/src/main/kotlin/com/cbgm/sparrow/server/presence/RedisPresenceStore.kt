package com.cbgm.sparrow.server.presence

import com.cbgm.sparrow.server.protocol.InvitationRoute
import com.cbgm.sparrow.server.protocol.InvitationRouteResult
import com.cbgm.sparrow.server.protocol.serverJson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import redis.clients.jedis.Jedis
import redis.clients.jedis.JedisPool
import java.net.URI
import java.util.Base64

internal class RedisPresenceStore(
    redisUrl: String,
    redisPassword: String?,
    private val keyPrefix: String,
    private val now: () -> Long = System::currentTimeMillis
) : PresenceStorage {
    private val pool: JedisPool

    override val persistenceMode: String = "redis"

    init {
        require(redisUrl.isNotBlank()) {
            "Presence Redis URL must not be blank"
        }
        require(keyPrefix.isNotBlank()) {
            "Presence Redis key prefix must not be blank"
        }
        pool = JedisPool(redisUri(redisUrl, redisPassword))
        pool.resource.use { jedis ->
            jedis.ping()
        }
    }

    override suspend fun bindAliases(
        deviceRoutingId: String,
        claims: AliasClaims
    ): List<String> =
        claims.aliases.filter { alias ->
            withJedis { jedis ->
                jedis.eval(
                    BIND_ALIAS_SCRIPT,
                    listOf(ownerKey(alias)),
                    listOf(
                        deviceRoutingId,
                        if (claims.isProven(alias)) "1" else "0",
                        ALIAS_BINDING_LIFETIME_MILLISECONDS.toString()
                    )
                ) as Long
            } == BOUND_RESULT
        }

    override suspend fun saveInvitationRoute(
        deviceRoutingId: String,
        invitationRoute: InvitationRoute?
    ) {
        val key = invitationRouteKey(deviceRoutingId)
        withJedis { jedis ->
            when (invitationRoute) {
                null -> jedis.del(key)
                else -> jedis.psetex(key, ALIAS_BINDING_LIFETIME_MILLISECONDS, serverJson.encodeToString(invitationRoute))
            }
        }
    }

    override suspend fun invitationRoute(routingId: String): InvitationRouteResult? {
        val device = aliasOwner(routingId) ?: routingId
        return withJedis { jedis -> jedis.get(invitationRouteKey(device)) }
            ?.let { encoded -> runCatching { serverJson.decodeFromString<InvitationRoute>(encoded) }.getOrNull() }
            ?.takeIf { invitation -> invitation.route.expiresAtEpochMilliseconds > now() }
            ?.let { invitation -> InvitationRouteResult(device, invitation) }
    }

    override suspend fun aliasOwner(alias: String): String? = withJedis { jedis -> jedis.get(ownerKey(alias)) }

    override fun close() {
        pool.close()
    }

    private suspend fun <T> withJedis(block: (Jedis) -> T): T =
        withContext(Dispatchers.IO) {
            pool.resource.use(block)
        }

    private fun routingPrefix(routingId: String): String =
        "$keyPrefix:route:${Base64.getUrlEncoder().withoutPadding().encodeToString(routingId.encodeToByteArray())}"

    private fun ownerKey(alias: String): String = "${routingPrefix(alias)}:owner"

    private fun invitationRouteKey(deviceRoutingId: String): String = "${routingPrefix(deviceRoutingId)}:invitation-route"

    private fun redisUri(
        redisUrl: String,
        redisPassword: String?
    ): URI {
        val uri = URI.create(redisUrl)
        if (redisPassword == null) {
            return uri
        }
        require(uri.rawUserInfo == null) {
            "Presence Redis credentials must be configured either in the URL or as a secret, not both"
        }
        return URI(
            uri.scheme,
            ":$redisPassword",
            uri.host,
            uri.port,
            uri.path,
            uri.query,
            uri.fragment
        )
    }

    private companion object {
        const val BOUND_RESULT = 1L

        // KEYS[1] the alias owner; ARGV[1] device routing ID, ARGV[2] alias proven, ARGV[3] binding lifetime.
        // Returns 1 when bound and 0 when the alias stays free or with its owner.
        val BIND_ALIAS_SCRIPT =
            """
            local owner = redis.call('GET', KEYS[1])
            if owner ~= ARGV[1] and ARGV[2] ~= '1' then
                return 0
            end
            redis.call('SET', KEYS[1], ARGV[1], 'PX', ARGV[3])
            return 1
            """.trimIndent()
    }
}
