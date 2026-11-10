package com.cbgm.sparrow.server.push

import com.cbgm.sparrow.server.persistence.ServiceEnvironment
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
                trustProxyHeaders =
                    ServiceEnvironment.string("TRUST_PROXY_HEADERS", "false").toBoolean()
            )

        private const val DEFAULT_DATABASE_MAXIMUM_POOL_SIZE = 10
        private const val DEFAULT_MAXIMUM_ENVELOPES = 100_000
        private const val DEFAULT_ENVELOPE_RETENTION_MILLISECONDS =
            7L * 24L * 60L * 60L * 1_000L
    }
}
