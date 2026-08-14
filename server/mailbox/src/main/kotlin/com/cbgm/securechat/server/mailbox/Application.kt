package com.cbgm.securechat.server.mailbox

import com.cbgm.securechat.server.observability.installServerObservability
import com.cbgm.securechat.server.persistence.ServiceEnvironment
import com.cbgm.securechat.server.protocol.serverJson
import com.cbgm.securechat.server.security.BoundedRateLimiter
import com.cbgm.securechat.server.security.RateLimitPolicy
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStopped
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.forwardedheaders.XForwardedHeaders

fun main() {
    embeddedServer(
        factory = Netty,
        host = "0.0.0.0",
        port = ServiceEnvironment.int("PORT", DEFAULT_MAILBOX_PORT),
        module = { mailboxModule() }
    ).start(wait = true)
}

fun Application.mailboxModule(
    config: MailboxConfig = MailboxConfig.fromEnvironment(),
    store: MailboxStorage = createMailboxStorage(config),
    pushNotifier: MailboxPushNotifier = MailboxPushNotifier.fromEnvironment()
) {
    configureMailboxLifecycle(store, pushNotifier)
    configureMailboxPlugins(config, store)
    configureMailboxRoutes(
        config = config,
        store = store,
        pushNotifier = pushNotifier,
        creationRateLimiter = BoundedRateLimiter(config.creationRateLimit)
    )
}

private fun Application.configureMailboxLifecycle(
    store: MailboxStorage,
    pushNotifier: MailboxPushNotifier
) {
    monitor.subscribe(ApplicationStopped) {
        store.close()
        pushNotifier.close()
    }
}

private fun Application.configureMailboxPlugins(
    config: MailboxConfig,
    store: MailboxStorage
) {
    installServerObservability("mailbox") {
        store.mailboxCount()
        true
    }
    install(ContentNegotiation) { json(serverJson) }
    if (config.trustProxyHeaders) {
        install(XForwardedHeaders)
    }
}

data class MailboxConfig(
    val databaseUrl: String?,
    val databaseUser: String,
    val databasePassword: String,
    val databaseMaximumPoolSize: Int,
    val maximumEnvelopeBytes: Int,
    val maximumMailboxBytes: Long,
    val maximumMailboxes: Int = DEFAULT_MAXIMUM_MAILBOXES,
    val maximumMailboxesPerClient: Int = DEFAULT_MAXIMUM_MAILBOXES_PER_CLIENT,
    val creationRateLimit: RateLimitPolicy =
        RateLimitPolicy(
            maximumRequests = DEFAULT_CREATION_RATE_LIMIT_REQUESTS,
            windowMilliseconds = DEFAULT_CREATION_RATE_LIMIT_WINDOW_MILLISECONDS
        ),
    val trustProxyHeaders: Boolean = false
) {
    init {
        require(databaseMaximumPoolSize > 0) {
            "Mailbox database maximum pool size must be positive"
        }
        require(maximumEnvelopeBytes > 0) {
            "Maximum envelope bytes must be positive"
        }
        require(maximumMailboxBytes > 0L) {
            "Maximum mailbox bytes must be positive"
        }
        require(maximumMailboxes > 0) { "Maximum mailbox count must be positive" }
        require(maximumMailboxesPerClient > 0) {
            "Per-client mailbox count must be positive"
        }
    }

    companion object {
        fun fromEnvironment(): MailboxConfig =
            MailboxConfig(
                databaseUrl = System.getenv("MAILBOX_DATABASE_URL")?.takeIf(String::isNotBlank),
                databaseUser = System.getenv("MAILBOX_DATABASE_USER").orEmpty(),
                databasePassword = ServiceEnvironment.secret("MAILBOX_DATABASE_PASSWORD").orEmpty(),
                databaseMaximumPoolSize =
                    System.getenv("MAILBOX_DATABASE_MAXIMUM_POOL_SIZE")?.toIntOrNull()
                        ?: DEFAULT_DATABASE_MAXIMUM_POOL_SIZE,
                maximumEnvelopeBytes =
                    System.getenv("MAILBOX_MAXIMUM_ENVELOPE_BYTES")?.toIntOrNull()
                        ?: DEFAULT_MAXIMUM_ENVELOPE_BYTES,
                maximumMailboxBytes =
                    System.getenv("MAILBOX_MAXIMUM_MAILBOX_BYTES")?.toLongOrNull()
                        ?: DEFAULT_MAXIMUM_MAILBOX_BYTES,
                maximumMailboxes =
                    ServiceEnvironment.int("MAILBOX_MAXIMUM_MAILBOXES", DEFAULT_MAXIMUM_MAILBOXES),
                maximumMailboxesPerClient =
                    ServiceEnvironment.int(
                        "MAILBOX_MAXIMUM_MAILBOXES_PER_CLIENT",
                        DEFAULT_MAXIMUM_MAILBOXES_PER_CLIENT
                    ),
                creationRateLimit =
                    RateLimitPolicy(
                        maximumRequests =
                            ServiceEnvironment.int(
                                "MAILBOX_CREATION_RATE_LIMIT_REQUESTS",
                                DEFAULT_CREATION_RATE_LIMIT_REQUESTS
                            ),
                        windowMilliseconds =
                            ServiceEnvironment.long(
                                "MAILBOX_CREATION_RATE_LIMIT_WINDOW_MILLISECONDS",
                                DEFAULT_CREATION_RATE_LIMIT_WINDOW_MILLISECONDS
                            ),
                        maximumTrackedClients =
                            ServiceEnvironment.int(
                                "MAILBOX_RATE_LIMIT_MAXIMUM_TRACKED_CLIENTS",
                                DEFAULT_RATE_LIMIT_MAXIMUM_TRACKED_CLIENTS
                            )
                    ),
                trustProxyHeaders =
                    ServiceEnvironment.string("TRUST_PROXY_HEADERS", "false").toBoolean()
            )

        private const val DEFAULT_DATABASE_MAXIMUM_POOL_SIZE = 10
        private const val DEFAULT_MAXIMUM_ENVELOPE_BYTES = 1_048_576
        private const val DEFAULT_MAXIMUM_MAILBOX_BYTES = 100L * 1_048_576L
        private const val DEFAULT_MAXIMUM_MAILBOXES = 100_000
        private const val DEFAULT_MAXIMUM_MAILBOXES_PER_CLIENT = 100
        private const val DEFAULT_CREATION_RATE_LIMIT_REQUESTS = 30
        private const val DEFAULT_CREATION_RATE_LIMIT_WINDOW_MILLISECONDS = 60L * 60L * 1_000L
        private const val DEFAULT_RATE_LIMIT_MAXIMUM_TRACKED_CLIENTS = 100_000
    }
}

internal fun createMailboxStorage(config: MailboxConfig): MailboxStorage {
    val databaseUrl = config.databaseUrl
    if (databaseUrl == null) {
        return MailboxStore(
            maximumEnvelopeBytes = config.maximumEnvelopeBytes,
            maximumMailboxBytes = config.maximumMailboxBytes
        )
    }

    val database =
        PostgresMailboxDatabase(
            PostgresMailboxDatabaseConfig(
                jdbcUrl = databaseUrl,
                username = config.databaseUser,
                password = config.databasePassword,
                maximumPoolSize = config.databaseMaximumPoolSize
            )
        )
    return PostgresMailboxStore(
        database = database,
        maximumEnvelopeBytes = config.maximumEnvelopeBytes,
        maximumMailboxBytes = config.maximumMailboxBytes
    )
}

private const val DEFAULT_MAILBOX_PORT = 8092
