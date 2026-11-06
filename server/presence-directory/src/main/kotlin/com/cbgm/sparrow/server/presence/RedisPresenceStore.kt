package com.cbgm.sparrow.server.presence

import com.cbgm.sparrow.server.protocol.ClientRoute
import com.cbgm.sparrow.server.protocol.ClientRouteRegistration
import com.cbgm.sparrow.server.protocol.ClientRoutingResult
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
    private val maximumTtlMilliseconds: Long,
    private val keyPrefix: String,
    private val now: () -> Long = System::currentTimeMillis
) : PresenceStorage {
    private data class RoutingKeys(
        val generation: String,
        val routes: String,
        val expiries: String,
        val owner: String
    )

    private val pool: JedisPool

    override val persistenceMode: String = "redis"

    init {
        require(redisUrl.isNotBlank()) {
            "Presence Redis URL must not be blank"
        }
        require(maximumTtlMilliseconds > 0L) {
            "Maximum route TTL must be positive"
        }
        require(keyPrefix.isNotBlank()) {
            "Presence Redis key prefix must not be blank"
        }
        pool = JedisPool(redisUri(redisUrl, redisPassword))
        pool.resource.use { jedis ->
            jedis.ping()
        }
    }

    override suspend fun register(registration: ClientRouteRegistration): PresenceResult {
        val currentTime = now()
        validatePresenceRegistration(registration, maximumTtlMilliseconds, currentTime)?.let { return it }
        val route = registration.route.copy(aliases = null)
        val keys = routingKeys(route.routingId)
        val result =
            withJedis { jedis ->
                jedis.eval(
                    REGISTER_SCRIPT,
                    listOf(keys.generation, keys.routes, keys.expiries, routingIdsKey),
                    listOf(
                        route.routingId,
                        currentTime.toString(),
                        route.generation.toString(),
                        route.connectionId,
                        serverJson.encodeToString(route),
                        route.expiresAtEpochMilliseconds.toString()
                    )
                ) as Long
            }
        return if (result == REGISTERED_RESULT) PresenceResult.Accepted else PresenceResult.Rejected("STALE_GENERATION")
    }

    override suspend fun bindAliases(
        deviceRoutingId: String,
        claims: AliasClaims
    ): List<String> =
        claims.aliases.filter { alias ->
            withJedis { jedis ->
                jedis.eval(
                    BIND_ALIAS_SCRIPT,
                    listOf(routingKeys(alias).owner),
                    listOf(
                        deviceRoutingId,
                        if (claims.isProven(alias)) "1" else "0",
                        if (claims.allowsFreeAlias(alias)) "1" else "0",
                        ALIAS_BINDING_LIFETIME_MILLISECONDS.toString()
                    )
                ) as Long
            } == REGISTERED_RESULT
        }

    override suspend fun remove(routingId: String, connectionId: String) {
        removeRoute(routingId, connectionId, now())
    }

    private suspend fun removeRoute(
        routingId: String,
        connectionId: String,
        currentTime: Long
    ) {
        val keys = routingKeys(routingId)
        withJedis { jedis ->
            jedis.eval(
                REMOVE_SCRIPT,
                listOf(keys.generation, keys.routes, keys.expiries, routingIdsKey),
                listOf(routingId, currentTime.toString(), connectionId)
            )
        }
    }

    override suspend fun resolve(routingId: String): ClientRoutingResult {
        val owner = withJedis { jedis -> jedis.get(routingKeys(routingId).owner) }
        return ClientRoutingResult(
            routingId = routingId,
            routes = resolveRoutes(owner ?: routingId),
            ownerRoutingId = owner
        )
    }

    private suspend fun resolveRoutes(routingId: String): List<ClientRoute> {
        val keys = routingKeys(routingId)
        val routes =
            withJedis { jedis ->
                @Suppress("UNCHECKED_CAST")
                val encodedRoutes =
                    jedis.eval(
                        RESOLVE_SCRIPT,
                        listOf(keys.generation, keys.routes, keys.expiries, routingIdsKey),
                        listOf(routingId, now().toString())
                    ) as List<String>
                encodedRoutes
                    .map { encoded -> serverJson.decodeFromString<ClientRoute>(encoded) }
                    .sortedBy(ClientRoute::nodeId)
            }
        return routes
    }

    override suspend fun routeCount(): Int =
        withJedis { jedis ->
            jedis.smembers(routingIdsKey).sumOf { routingId ->
                val keys = routingKeys(routingId)
                val count =
                    jedis.eval(
                        COUNT_SCRIPT,
                        listOf(keys.generation, keys.routes, keys.expiries, routingIdsKey),
                        listOf(routingId, now().toString())
                    ) as Long
                count.toInt()
            }
        }

    override fun close() {
        pool.close()
    }

    private suspend fun <T> withJedis(block: (Jedis) -> T): T =
        withContext(Dispatchers.IO) {
            pool.resource.use(block)
        }

    private fun routingKeys(routingId: String): RoutingKeys {
        val encodedRoutingId =
            Base64.getUrlEncoder().withoutPadding().encodeToString(routingId.encodeToByteArray())
        val routingPrefix = "$keyPrefix:route:$encodedRoutingId"
        return RoutingKeys(
            generation = "$routingPrefix:generation",
            routes = "$routingPrefix:routes",
            expiries = "$routingPrefix:expiries",
            owner = "$routingPrefix:owner"
        )
    }

    private val routingIdsKey: String
        get() = "$keyPrefix:routing-ids"

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
        const val REGISTERED_RESULT = 1L

        // Returns 0 for a stale generation and 1 when registered.
        val REGISTER_SCRIPT =
            """
            local expired = redis.call('ZRANGEBYSCORE', KEYS[3], '-inf', ARGV[2])
            for _, connectionId in ipairs(expired) do
                redis.call('HDEL', KEYS[2], connectionId)
            end
            if #expired > 0 then
                redis.call('ZREM', KEYS[3], unpack(expired))
            end
            if redis.call('ZCARD', KEYS[3]) == 0 then
                redis.call('DEL', KEYS[1], KEYS[2], KEYS[3])
                redis.call('SREM', KEYS[4], ARGV[1])
            end

            local currentGeneration = redis.call('GET', KEYS[1])
            if currentGeneration and tonumber(ARGV[3]) < tonumber(currentGeneration) then
                return 0
            end
            if not currentGeneration or tonumber(ARGV[3]) > tonumber(currentGeneration) then
                redis.call('DEL', KEYS[1], KEYS[2], KEYS[3])
                redis.call('SET', KEYS[1], ARGV[3])
            end

            redis.call('HSET', KEYS[2], ARGV[4], ARGV[5])
            redis.call('ZADD', KEYS[3], ARGV[6], ARGV[4])
            redis.call('SADD', KEYS[4], ARGV[1])
            local latest = redis.call('ZREVRANGE', KEYS[3], 0, 0, 'WITHSCORES')
            redis.call('PEXPIREAT', KEYS[1], latest[2])
            redis.call('PEXPIREAT', KEYS[2], latest[2])
            redis.call('PEXPIREAT', KEYS[3], latest[2])
            return 1
            """.trimIndent()

        // KEYS[1] the alias owner; ARGV[1] device routing ID, ARGV[2] alias proven, ARGV[3] free alias allowed,
        // ARGV[4] binding lifetime. Returns 1 when bound and 0 when the alias stays with its owner.
        val BIND_ALIAS_SCRIPT =
            """
            local owner = redis.call('GET', KEYS[1])
            if not owner then
                if ARGV[3] ~= '1' then
                    return 0
                end
            elseif owner ~= ARGV[1] then
                if ARGV[2] ~= '1' then
                    return 0
                end
            end
            redis.call('SET', KEYS[1], ARGV[1], 'PX', ARGV[4])
            return 1
            """.trimIndent()

        val REMOVE_SCRIPT =
            """
            local expired = redis.call('ZRANGEBYSCORE', KEYS[3], '-inf', ARGV[2])
            for _, expiredConnectionId in ipairs(expired) do
                redis.call('HDEL', KEYS[2], expiredConnectionId)
            end
            if #expired > 0 then
                redis.call('ZREM', KEYS[3], unpack(expired))
            end
            redis.call('HDEL', KEYS[2], ARGV[3])
            redis.call('ZREM', KEYS[3], ARGV[3])

            if redis.call('ZCARD', KEYS[3]) == 0 then
                redis.call('DEL', KEYS[1], KEYS[2], KEYS[3])
                redis.call('SREM', KEYS[4], ARGV[1])
                return 0
            end
            local latest = redis.call('ZREVRANGE', KEYS[3], 0, 0, 'WITHSCORES')
            redis.call('PEXPIREAT', KEYS[1], latest[2])
            redis.call('PEXPIREAT', KEYS[2], latest[2])
            redis.call('PEXPIREAT', KEYS[3], latest[2])
            return 1
            """.trimIndent()

        val RESOLVE_SCRIPT =
            """
            local expired = redis.call('ZRANGEBYSCORE', KEYS[3], '-inf', ARGV[2])
            for _, connectionId in ipairs(expired) do
                redis.call('HDEL', KEYS[2], connectionId)
            end
            if #expired > 0 then
                redis.call('ZREM', KEYS[3], unpack(expired))
            end
            if redis.call('ZCARD', KEYS[3]) == 0 then
                redis.call('DEL', KEYS[1], KEYS[2], KEYS[3])
                redis.call('SREM', KEYS[4], ARGV[1])
                return {}
            end
            return redis.call('HVALS', KEYS[2])
            """.trimIndent()

        val COUNT_SCRIPT =
            """
            local expired = redis.call('ZRANGEBYSCORE', KEYS[3], '-inf', ARGV[2])
            for _, connectionId in ipairs(expired) do
                redis.call('HDEL', KEYS[2], connectionId)
            end
            if #expired > 0 then
                redis.call('ZREM', KEYS[3], unpack(expired))
            end
            local count = redis.call('ZCARD', KEYS[3])
            if count == 0 then
                redis.call('DEL', KEYS[1], KEYS[2], KEYS[3])
                redis.call('SREM', KEYS[4], ARGV[1])
            end
            return count
            """.trimIndent()
    }
}
