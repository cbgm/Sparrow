package com.cbgm.securechat.server.push

import com.cbgm.securechat.server.persistence.ServiceEnvironment
import com.cbgm.securechat.server.security.RateLimitPolicy
import io.ktor.server.application.Application
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty

private const val DEFAULT_PUSH_PORT = 8095

fun main() {
    embeddedServer(
        factory = Netty,
        host = "0.0.0.0",
        port = ServiceEnvironment.int("PORT", DEFAULT_PUSH_PORT),
        module = { pushModule() }
    ).start(wait = true)
}

fun Application.pushModule(
    config: PushConfig = PushConfig.fromEnvironment()
) {
    val runtime = createPushRuntime(config)

    configurePushLifecycle(runtime)
    installPushPlugins(runtime, config)
    installPushRoutes(runtime, config)
}

data class PushConfig(
    val pushInternalApiToken: String?,
    val nodeRegistryUrl: String?,
    val presenceDirectoryUrl: String?,
    val databaseUrl: String?,
    val databaseUser: String,
    val databasePassword: String,
    val databaseMaximumPoolSize: Int,
    val maximumEnvelopes: Int,
    val envelopeRetentionMilliseconds: Long,
    val wakeUpLifetimeMilliseconds: Long,
    val deviceRegistrationRateLimit: RateLimitPolicy =
        RateLimitPolicy(
            maximumRequests = DEFAULT_DEVICE_REGISTRATION_RATE_LIMIT_REQUESTS,
            windowMilliseconds = DEFAULT_DEVICE_REGISTRATION_RATE_LIMIT_WINDOW_MILLISECONDS
        ),
    val trustProxyHeaders: Boolean = false
) {
    init {
        require(databaseMaximumPoolSize > 0) {
            "Push database maximum pool size must be positive"
        }
        require(maximumEnvelopes > 0) {
            "Maximum envelope count must be positive"
        }
        require(envelopeRetentionMilliseconds > 0L) {
            "Envelope retention must be positive"
        }
        require(wakeUpLifetimeMilliseconds > 0L) {
            "Wake-up lifetime must be positive"
        }
    }

    companion object {
        fun fromEnvironment(): PushConfig =
            PushConfig(
                pushInternalApiToken =
                    ServiceEnvironment.secret("PUSH_INTERNAL_API_TOKEN")
                        ?: ServiceEnvironment.secret("INTERNAL_API_TOKEN"),
                nodeRegistryUrl =
                    System.getenv("NODE_REGISTRY_URL")?.takeIf(String::isNotBlank),
                presenceDirectoryUrl =
                    System.getenv("PRESENCE_DIRECTORY_URL")?.takeIf(String::isNotBlank),
                databaseUrl = System.getenv("PUSH_DATABASE_URL")?.takeIf(String::isNotBlank),
                databaseUser = System.getenv("PUSH_DATABASE_USER").orEmpty(),
                databasePassword =
                    ServiceEnvironment.secret("PUSH_DATABASE_PASSWORD").orEmpty(),
                databaseMaximumPoolSize =
                    System.getenv("PUSH_DATABASE_MAXIMUM_POOL_SIZE")?.toIntOrNull()
                        ?: DEFAULT_DATABASE_MAXIMUM_POOL_SIZE,
                maximumEnvelopes =
                    System.getenv("PUSH_MAXIMUM_ENVELOPES")?.toIntOrNull()
                        ?: DEFAULT_MAXIMUM_ENVELOPES,
                envelopeRetentionMilliseconds =
                    System.getenv("PUSH_ENVELOPE_RETENTION_MILLISECONDS")?.toLongOrNull()
                        ?: DEFAULT_ENVELOPE_RETENTION_MILLISECONDS,
                wakeUpLifetimeMilliseconds =
                    System.getenv("PUSH_WAKE_UP_LIFETIME_MILLISECONDS")?.toLongOrNull()
                        ?: DEFAULT_WAKE_UP_LIFETIME_MILLISECONDS,
                deviceRegistrationRateLimit =
                    RateLimitPolicy(
                        maximumRequests =
                            ServiceEnvironment.int(
                                "PUSH_DEVICE_REGISTRATION_RATE_LIMIT_REQUESTS",
                                DEFAULT_DEVICE_REGISTRATION_RATE_LIMIT_REQUESTS
                            ),
                        windowMilliseconds =
                            ServiceEnvironment.long(
                                "PUSH_DEVICE_REGISTRATION_RATE_LIMIT_WINDOW_MILLISECONDS",
                                DEFAULT_DEVICE_REGISTRATION_RATE_LIMIT_WINDOW_MILLISECONDS
                            ),
                        maximumTrackedClients =
                            ServiceEnvironment.int(
                                "PUSH_RATE_LIMIT_MAXIMUM_TRACKED_CLIENTS",
                                DEFAULT_RATE_LIMIT_MAXIMUM_TRACKED_CLIENTS
                            )
                    ),
                trustProxyHeaders =
                    ServiceEnvironment.string("TRUST_PROXY_HEADERS", "false").toBoolean()
            )

        private const val DEFAULT_DATABASE_MAXIMUM_POOL_SIZE = 10
        private const val DEFAULT_MAXIMUM_ENVELOPES = 100_000
        private const val DEFAULT_ENVELOPE_RETENTION_MILLISECONDS =
            7L * 24L * 60L * 60L * 1_000L
        private const val DEFAULT_WAKE_UP_LIFETIME_MILLISECONDS =
            15L * 60L * 1_000L
        private const val DEFAULT_DEVICE_REGISTRATION_RATE_LIMIT_REQUESTS = 60
        private const val DEFAULT_DEVICE_REGISTRATION_RATE_LIMIT_WINDOW_MILLISECONDS = 60_000L
        private const val DEFAULT_RATE_LIMIT_MAXIMUM_TRACKED_CLIENTS = 100_000
    }
}
