package com.cbgm.securechat.server.federation

import com.cbgm.securechat.server.persistence.ServiceEnvironment
import com.cbgm.securechat.server.persistence.controlPlaneUrlsFromEnvironment
import com.cbgm.securechat.server.security.NodeIdentity
import com.cbgm.securechat.server.security.NodeIdentityStore
import com.cbgm.securechat.server.security.RateLimitPolicy
import io.ktor.client.HttpClient
import io.ktor.server.application.Application
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import java.nio.file.Path

private const val DEFAULT_FEDERATION_PORT = 8093

fun main() {
    val identity =
        NodeIdentityStore(
            Path.of(ServiceEnvironment.string("NODE_IDENTITY_PATH", ".securechat-server/node.identity"))
        ).loadOrCreate()

    embeddedServer(
        factory = Netty,
        host = "0.0.0.0",
        port = ServiceEnvironment.int("PORT", DEFAULT_FEDERATION_PORT),
        module = { federationModule(identity) }
    ).start(wait = true)
}

fun Application.federationModule(
    identity: NodeIdentity,
    config: FederationConfig = FederationConfig.fromEnvironment(),
    suppliedHttpClient: HttpClient? = null
) {
    val managedHttpClient = createManagedHttpClient(suppliedHttpClient)
    val runtime = createFederationRuntime(identity, config, managedHttpClient.client)

    configureFederationLifecycle(runtime, identity, config, managedHttpClient.owned)
    installFederationPlugins(runtime, config)
    installFederationRoutes(runtime, config)
}

data class FederationConfig(
    val databaseUrl: String?,
    val databaseUser: String,
    val databasePassword: String,
    val databaseMaximumPoolSize: Int,
    val controlPlaneUrls: List<String>,
    val nodeRegistryUrl: String,
    val presenceDirectoryUrl: String,
    val gatewayInternalUrl: String,
    val federationInternalApiToken: String?,
    val gatewayInternalApiToken: String?,
    val maximumDeduplicationEntries: Int,
    val registerNode: Boolean,
    val clientEndpoint: String,
    val federationEndpoint: String,
    val mailboxEndpoint: String,
    val outboundRetryPollIntervalMilliseconds: Long,
    val outboundRetryBaseDelayMilliseconds: Long,
    val outboundRetryMaximumDelayMilliseconds: Long,
    val outboundRetryBatchSize: Int,
    val incomingRateLimit: RateLimitPolicy =
        RateLimitPolicy(
            maximumRequests = DEFAULT_INCOMING_RATE_LIMIT_REQUESTS,
            windowMilliseconds = DEFAULT_INCOMING_RATE_LIMIT_WINDOW_MILLISECONDS
        ),
    val trustProxyHeaders: Boolean = false
) {
    init {
        require(databaseMaximumPoolSize > 0)
        require(outboundRetryPollIntervalMilliseconds > 0L)
        require(outboundRetryBaseDelayMilliseconds > 0L)
        require(outboundRetryMaximumDelayMilliseconds >= outboundRetryBaseDelayMilliseconds)
        require(outboundRetryBatchSize > 0)
    }

    companion object {
        fun fromEnvironment(): FederationConfig =
            FederationConfig(
                databaseUrl = System.getenv("FEDERATION_DATABASE_URL")?.takeIf(String::isNotBlank),
                databaseUser = System.getenv("FEDERATION_DATABASE_USER").orEmpty(),
                databasePassword =
                    ServiceEnvironment.secret("FEDERATION_DATABASE_PASSWORD").orEmpty(),
                databaseMaximumPoolSize =
                    ServiceEnvironment.int(
                        "FEDERATION_DATABASE_MAXIMUM_POOL_SIZE",
                        DEFAULT_DATABASE_MAXIMUM_POOL_SIZE
                    ),
                controlPlaneUrls =
                    System.getenv("CONTROL_PLANE_URLS")
                        ?.takeIf(String::isNotBlank)
                        ?.let { value ->
                            controlPlaneUrlsFromEnvironment(
                                legacyEnvironmentNames = emptyList(),
                                defaultUrl = value
                            )
                        }
                        ?: emptyList(),
                nodeRegistryUrl =
                    ServiceEnvironment.string("NODE_REGISTRY_URL", "http://localhost:8090"),
                presenceDirectoryUrl =
                    ServiceEnvironment.string(
                        "PRESENCE_DIRECTORY_URL",
                        "http://localhost:8091"
                    ),
                gatewayInternalUrl =
                    ServiceEnvironment.string("GATEWAY_INTERNAL_URL", "http://localhost:8094"),
                federationInternalApiToken =
                    ServiceEnvironment.secret("FEDERATION_INTERNAL_API_TOKEN")
                        ?: ServiceEnvironment.secret("INTERNAL_API_TOKEN"),
                gatewayInternalApiToken =
                    ServiceEnvironment.secret("GATEWAY_INTERNAL_API_TOKEN")
                        ?: ServiceEnvironment.secret("INTERNAL_API_TOKEN"),
                maximumDeduplicationEntries =
                    ServiceEnvironment.int(
                        "MAX_DEDUPLICATION_ENTRIES",
                        DEFAULT_MAXIMUM_DEDUPLICATION_ENTRIES
                    ),
                registerNode = ServiceEnvironment.string("REGISTER_NODE", "true").toBoolean(),
                clientEndpoint =
                    ServiceEnvironment.string("CLIENT_ENDPOINT", "ws://localhost:8094/relay"),
                federationEndpoint =
                    ServiceEnvironment.string("FEDERATION_ENDPOINT", "http://localhost:8093"),
                mailboxEndpoint =
                    ServiceEnvironment.string("MAILBOX_ENDPOINT", "http://localhost:8092"),
                outboundRetryPollIntervalMilliseconds =
                    ServiceEnvironment.long(
                        "FEDERATION_RETRY_POLL_INTERVAL_MILLISECONDS",
                        DEFAULT_RETRY_POLL_INTERVAL_MILLISECONDS
                    ),
                outboundRetryBaseDelayMilliseconds =
                    ServiceEnvironment.long(
                        "FEDERATION_RETRY_BASE_DELAY_MILLISECONDS",
                        DEFAULT_RETRY_BASE_DELAY_MILLISECONDS
                    ),
                outboundRetryMaximumDelayMilliseconds =
                    ServiceEnvironment.long(
                        "FEDERATION_RETRY_MAXIMUM_DELAY_MILLISECONDS",
                        DEFAULT_RETRY_MAXIMUM_DELAY_MILLISECONDS
                    ),
                outboundRetryBatchSize =
                    ServiceEnvironment.int(
                        "FEDERATION_RETRY_BATCH_SIZE",
                        DEFAULT_RETRY_BATCH_SIZE
                    ),
                incomingRateLimit =
                    RateLimitPolicy(
                        maximumRequests =
                            ServiceEnvironment.int(
                                "FEDERATION_INCOMING_RATE_LIMIT_REQUESTS",
                                DEFAULT_INCOMING_RATE_LIMIT_REQUESTS
                            ),
                        windowMilliseconds =
                            ServiceEnvironment.long(
                                "FEDERATION_INCOMING_RATE_LIMIT_WINDOW_MILLISECONDS",
                                DEFAULT_INCOMING_RATE_LIMIT_WINDOW_MILLISECONDS
                            ),
                        maximumTrackedClients =
                            ServiceEnvironment.int(
                                "FEDERATION_RATE_LIMIT_MAXIMUM_TRACKED_CLIENTS",
                                DEFAULT_RATE_LIMIT_MAXIMUM_TRACKED_CLIENTS
                            )
                    ),
                trustProxyHeaders =
                    ServiceEnvironment.string("TRUST_PROXY_HEADERS", "false").toBoolean()
            )

        private const val DEFAULT_DATABASE_MAXIMUM_POOL_SIZE = 10
        private const val DEFAULT_MAXIMUM_DEDUPLICATION_ENTRIES = 100_000
        private const val DEFAULT_RETRY_POLL_INTERVAL_MILLISECONDS = 1_000L
        private const val DEFAULT_RETRY_BASE_DELAY_MILLISECONDS = 5_000L
        private const val DEFAULT_RETRY_MAXIMUM_DELAY_MILLISECONDS = 5L * 60L * 1_000L
        private const val DEFAULT_RETRY_BATCH_SIZE = 100
        private const val DEFAULT_INCOMING_RATE_LIMIT_REQUESTS = 1_200
        private const val DEFAULT_INCOMING_RATE_LIMIT_WINDOW_MILLISECONDS = 60_000L
        private const val DEFAULT_RATE_LIMIT_MAXIMUM_TRACKED_CLIENTS = 100_000
    }
}

internal fun createOutboundEnvelopeStorage(config: FederationConfig): OutboundEnvelopeStorage {
    val databaseUrl = config.databaseUrl ?: return OutboundEnvelopeQueue()
    val database =
        PostgresOutboundEnvelopeDatabase(
            PostgresOutboundEnvelopeDatabaseConfig(
                jdbcUrl = databaseUrl,
                username = config.databaseUser,
                password = config.databasePassword,
                maximumPoolSize = config.databaseMaximumPoolSize
            )
        )
    return PostgresOutboundEnvelopeStorage(database)
}
