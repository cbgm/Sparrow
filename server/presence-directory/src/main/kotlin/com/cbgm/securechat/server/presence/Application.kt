package com.cbgm.securechat.server.presence

import com.cbgm.securechat.server.persistence.ServiceEnvironment
import io.ktor.server.application.Application
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty

private const val DEFAULT_PRESENCE_DIRECTORY_PORT = 8091

fun main() {
    embeddedServer(
        factory = Netty,
        host = "0.0.0.0",
        port = ServiceEnvironment.int("PORT", DEFAULT_PRESENCE_DIRECTORY_PORT),
        module = { presenceDirectoryModule() }
    ).start(wait = true)
}

fun Application.presenceDirectoryModule(
    store: PresenceStorage = createPresenceStorage(PresenceConfig.fromEnvironment())
) {
    val runtime = createPresenceRuntime(store)

    configurePresenceLifecycle(runtime)
    installPresencePlugins(runtime)
    installPresenceRoutes(runtime)
}

data class PresenceConfig(
    val redisUrl: String?,
    val redisPassword: String?,
    val redisKeyPrefix: String,
    val maximumTtlMilliseconds: Long
) {
    init {
        require(redisKeyPrefix.isNotBlank()) {
            "Presence Redis key prefix must not be blank"
        }
        require(maximumTtlMilliseconds > 0L) {
            "Maximum route TTL must be positive"
        }
    }

    companion object {
        fun fromEnvironment(): PresenceConfig =
            PresenceConfig(
                redisUrl = System.getenv("PRESENCE_REDIS_URL")?.takeIf(String::isNotBlank),
                redisPassword = ServiceEnvironment.secret("PRESENCE_REDIS_PASSWORD"),
                redisKeyPrefix =
                    System.getenv("PRESENCE_REDIS_KEY_PREFIX")?.takeIf(String::isNotBlank)
                        ?: DEFAULT_REDIS_KEY_PREFIX,
                maximumTtlMilliseconds =
                    System.getenv("PRESENCE_MAXIMUM_TTL_MILLISECONDS")?.toLongOrNull()
                        ?: DEFAULT_MAXIMUM_TTL_MILLISECONDS
            )

        private const val DEFAULT_REDIS_KEY_PREFIX = "securechat:presence"
        private const val DEFAULT_MAXIMUM_TTL_MILLISECONDS = 120_000L
    }
}

internal fun createPresenceStorage(config: PresenceConfig): PresenceStorage {
    val redisUrl = config.redisUrl
    return if (redisUrl == null) {
        PresenceStore(maximumTtlMilliseconds = config.maximumTtlMilliseconds)
    } else {
        RedisPresenceStore(
            redisUrl = redisUrl,
            redisPassword = config.redisPassword,
            maximumTtlMilliseconds = config.maximumTtlMilliseconds,
            keyPrefix = config.redisKeyPrefix
        )
    }
}
