package com.cbgm.sparrow.server.presence

import com.cbgm.sparrow.server.persistence.ServiceEnvironment
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
    installPresencePlugins()
    installPresenceRoutes(runtime)
}

data class PresenceConfig(
    val redisUrl: String?,
    val redisPassword: String?,
    val redisKeyPrefix: String
) {
    init {
        require(redisKeyPrefix.isNotBlank()) {
            "Presence Redis key prefix must not be blank"
        }
    }

    companion object {
        fun fromEnvironment(): PresenceConfig =
            PresenceConfig(
                redisUrl = System.getenv("PRESENCE_REDIS_URL")?.takeIf(String::isNotBlank),
                redisPassword = ServiceEnvironment.secret("PRESENCE_REDIS_PASSWORD"),
                redisKeyPrefix =
                    System.getenv("PRESENCE_REDIS_KEY_PREFIX")?.takeIf(String::isNotBlank)
                        ?: DEFAULT_REDIS_KEY_PREFIX
            )

        private const val DEFAULT_REDIS_KEY_PREFIX = "sparrow:presence"
    }
}

internal fun createPresenceStorage(config: PresenceConfig): PresenceStorage {
    val redisUrl = config.redisUrl
    return if (redisUrl == null) {
        PresenceStore()
    } else {
        RedisPresenceStore(
            redisUrl = redisUrl,
            redisPassword = config.redisPassword,
            keyPrefix = config.redisKeyPrefix
        )
    }
}
