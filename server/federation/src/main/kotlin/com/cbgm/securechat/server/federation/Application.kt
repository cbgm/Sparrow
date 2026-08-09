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
            Path.of(
                ServiceEnvironment.string(
                    "NODE_IDENTITY_PATH",
                    ".securechat-server/node.identity"
                )
            )
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
            with(ServiceEnvironment) {
                FederationConfig(
                    databaseUrl = optionalEnv("FEDERATION_DATABASE_URL"),
                    databaseUser = System.getenv("FEDERATION_DATABASE_USER").orEmpty(),
                    databasePassword = secret("FEDERATION_DATABASE_PASSWORD").orEmpty(),
                    databaseMaximumPoolSize =
                        int(
                            "FEDERATION_DATABASE_MAXIMUM_POOL_SIZE",
                            DEFAULT_DATABASE_MAXIMUM_POOL_SIZE
                        ),
                    controlPlaneUrls =
                        optionalEnv("CONTROL_PLANE_URLS")
                            ?.let { controlPlaneUrlsFromEnvironment(emptyList(), it) }
                            ?: emptyList(),
                    nodeRegistryUrl = string("NODE_REGISTRY_URL", "http://localhost:8090"),
                    presenceDirectoryUrl = string("PRESENCE_DIRECTORY_URL", "http://localhost:8091"),
                    gatewayInternalUrl = string("GATEWAY_INTERNAL_URL", "http://localhost:8094"),
                    federationInternalApiToken =
                        secret("FEDERATION_INTERNAL_API_TOKEN")
                            ?: secret("INTERNAL_API_TOKEN"),
                    gatewayInternalApiToken =
                        secret("GATEWAY_INTERNAL_API_TOKEN")
                            ?: secret("INTERNAL_API_TOKEN"),
                    maximumDeduplicationEntries =
                        int(
                            "MAX_DEDUPLICATION_ENTRIES",
                            DEFAULT_MAXIMUM_DEDUPLICATION_ENTRIES
                        ),
                    registerNode = boolean("REGISTER_NODE", true),
                    clientEndpoint = string("CLIENT_ENDPOINT", "ws://localhost:8094/relay"),
                    federationEndpoint = string("FEDERATION_ENDPOINT", "http://localhost:8093"),
                    mailboxEndpoint = string("MAILBOX_ENDPOINT", "http://localhost:8092"),
                    outboundRetryPollIntervalMilliseconds =
                        long(
                            "FEDERATION_RETRY_POLL_INTERVAL_MILLISECONDS",
                            DEFAULT_RETRY_POLL_INTERVAL_MILLISECONDS
                        ),
                    outboundRetryBaseDelayMilliseconds =
                        long(
                            "FEDERATION_RETRY_BASE_DELAY_MILLISECONDS",
                            DEFAULT_RETRY_BASE_DELAY_MILLISECONDS
                        ),
                    outboundRetryMaximumDelayMilliseconds =
                        long(
                            "FEDERATION_RETRY_MAXIMUM_DELAY_MILLISECONDS",
                            DEFAULT_RETRY_MAXIMUM_DELAY_MILLISECONDS
                        ),
                    outboundRetryBatchSize =
                        int(
                            "FEDERATION_RETRY_BATCH_SIZE",
                            DEFAULT_RETRY_BATCH_SIZE
                        ),
                    incomingRateLimit =
                        RateLimitPolicy(
                            maximumRequests =
                                int(
                                    "FEDERATION_INCOMING_RATE_LIMIT_REQUESTS",
                                    DEFAULT_INCOMING_RATE_LIMIT_REQUESTS
                                ),
                            windowMilliseconds =
                                long(
                                    "FEDERATION_INCOMING_RATE_LIMIT_WINDOW_MILLISECONDS",
                                    DEFAULT_INCOMING_RATE_LIMIT_WINDOW_MILLISECONDS
                                ),
                            maximumTrackedClients =
                                int(
                                    "FEDERATION_RATE_LIMIT_MAXIMUM_TRACKED_CLIENTS",
                                    DEFAULT_RATE_LIMIT_MAXIMUM_TRACKED_CLIENTS
                                )
                        ),
                    trustProxyHeaders = boolean("TRUST_PROXY_HEADERS", false)
                )
            }

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

internal fun ServiceEnvironment.boolean(key: String, default: Boolean): Boolean =
    string(key, default.toString()).toBoolean()

internal fun ServiceEnvironment.optionalEnv(key: String): String? =
    System.getenv(key)?.takeIf(String::isNotBlank)
