package com.cbgm.securechat.server.presence

import com.cbgm.securechat.server.protocol.ClientRoute
import com.cbgm.securechat.server.protocol.ClientRouteRegistration
import com.cbgm.securechat.server.protocol.ClientRoutingResult
import com.cbgm.securechat.server.protocol.serverJson
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
        val expiries: String
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
        val route = registration.route
        val routingIds = listOf(route.routingId) + route.aliases.orEmpty()
        val encodedRoute = serverJson.encodeToString(route)
        val results =
            routingIds.map { routingId ->
                val keys = routingKeys(routingId)
                withJedis { jedis ->
                    jedis.eval(
                        REGISTER_SCRIPT,
                        listOf(keys.generation, keys.routes, keys.expiries, routingIdsKey),
                        listOf(
                            routingId,
                            currentTime.toString(),
                            route.generation.toString(),
                            route.connectionId,
                            encodedRoute,
                            route.expiresAtEpochMilliseconds.toString(),
                            if (routingId == route.routingId) "1" else "0"
                        )
                    ) as Long
                }
            }
        return if (results.all { result -> result == REGISTERED_RESULT }) {
            PresenceResult.Accepted
        } else {
            routingIds.distinct().forEach { routingId ->
                removeRoute(routingId, route.connectionId, currentTime)
            }
            PresenceResult.Rejected("STALE_GENERATION")
        }
    }

    override suspend fun remove(routingId: String, connectionId: String) {
        val route = resolve(routingId).routes.firstOrNull { candidate -> candidate.connectionId == connectionId }
        val routingIds = listOf(route?.routingId ?: routingId) + route?.aliases.orEmpty()
        val currentTime = now()
        routingIds.distinct().forEach { currentRoutingId ->
            removeRoute(currentRoutingId, connectionId, currentTime)
        }
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
        return ClientRoutingResult(routingId = routingId, routes = routes)
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
            expiries = "$routingPrefix:expiries"
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
            if ARGV[7] == '1' then
                redis.call('SADD', KEYS[4], ARGV[1])
            end
            local latest = redis.call('ZREVRANGE', KEYS[3], 0, 0, 'WITHSCORES')
            redis.call('PEXPIREAT', KEYS[1], latest[2])
            redis.call('PEXPIREAT', KEYS[2], latest[2])
            redis.call('PEXPIREAT', KEYS[3], latest[2])
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
