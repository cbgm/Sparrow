package com.cbgm.sparrow.server.gateway

import com.cbgm.sparrow.server.persistence.ServiceEnvironment
import com.cbgm.sparrow.server.persistence.controlPlaneUrlsFromEnvironment
import com.cbgm.sparrow.server.security.NodeIdentity
import com.cbgm.sparrow.server.security.NodeIdentityStore
import io.ktor.client.HttpClient
import io.ktor.server.application.Application
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import java.nio.file.Path

private const val DEFAULT_GATEWAY_PORT = 8094

fun main() {
    val config = GatewayConfig.fromEnvironment()
    val identity = NodeIdentityStore(Path.of(config.nodeIdentityPath)).loadOrCreate()

    embeddedServer(
        factory = Netty,
        host = "0.0.0.0",
        port = config.port,
        module = { gatewayModule(identity, config) }
    ).start(wait = true)
}

fun Application.gatewayModule(
    identity: NodeIdentity,
    config: GatewayConfig = GatewayConfig.fromEnvironment(),
    suppliedHttpClient: HttpClient? = null
) {
    val runtime = createGatewayRuntime(identity, config, suppliedHttpClient)

    configureGatewayLifecycle(runtime)
    installGatewayPlugins(runtime, config)
    installGatewayRoutes(runtime, identity, config)
}

data class GatewayConfig(
    val port: Int,
    val nodeIdentityPath: String,
    val federationInternalUrl: String,
    val pushInternalUrl: String,
    val controlPlaneUrls: List<String>,
    val advertisedControlPlaneUrls: List<String>,
    val pushNodeApiUrl: String?,
    val presenceDirectoryUrl: String,
    val federationInternalApiToken: String?,
    val pushInternalApiToken: String?,
    val gatewayInternalApiToken: String?,
    val maximumFrameBytes: Long,
    val routeLifetimeMilliseconds: Long,
    val routeRefreshIntervalMilliseconds: Long,
    val blobStoragePath: String,
    val maximumBlobBytes: Long,
    val maximumBlobStorageBytes: Long,
    val maximumBlobRetentionMilliseconds: Long,
    val blobCleanupIntervalMilliseconds: Long,
    val blobUploadTicketLifetimeMilliseconds: Long
) {
    init {
        require(routeLifetimeMilliseconds > 0L) {
            "Route lifetime must be positive"
        }
        require(routeRefreshIntervalMilliseconds in 1 until routeLifetimeMilliseconds) {
            "Route refresh interval must be positive and shorter than the route lifetime"
        }
        require(blobStoragePath.isNotBlank()) { "Blob storage path must not be blank" }
        require(maximumBlobBytes > 0L) { "Maximum blob size must be positive" }
        require(maximumBlobStorageBytes >= maximumBlobBytes) {
            "Maximum blob storage must hold at least one maximum-size blob"
        }
        require(maximumBlobRetentionMilliseconds > 0L) { "Maximum blob retention must be positive" }
        require(blobCleanupIntervalMilliseconds > 0L) { "Blob cleanup interval must be positive" }
        require(blobUploadTicketLifetimeMilliseconds > 0L) { "Blob upload ticket lifetime must be positive" }
        require(blobUploadTicketLifetimeMilliseconds < maximumBlobRetentionMilliseconds) {
            "Blob upload tickets must expire before maximum blob retention"
        }
    }

    companion object {
        fun fromEnvironment(): GatewayConfig {
            val legacyToken = ServiceEnvironment.secret("INTERNAL_API_TOKEN")
            val controlPlaneUrls = configuredControlPlaneUrls()
            return GatewayConfig(
                port = ServiceEnvironment.int("PORT", DEFAULT_GATEWAY_PORT),
                nodeIdentityPath =
                    ServiceEnvironment.string(
                        "NODE_IDENTITY_PATH",
                        ".sparrow-server/node.identity"
                    ),
                federationInternalUrl =
                    ServiceEnvironment.string(
                        "FEDERATION_INTERNAL_URL",
                        "http://localhost:8093"
                    ),
                pushInternalUrl =
                    ServiceEnvironment.string(
                        "PUSH_INTERNAL_URL",
                        "http://localhost:8095"
                    ),
                controlPlaneUrls = controlPlaneUrls,
                advertisedControlPlaneUrls =
                    advertisedControlPlaneUrls(fallback = controlPlaneUrls),
                pushNodeApiUrl =
                    System.getenv("PUSH_NODE_API_URL")?.takeIf(String::isNotBlank),
                presenceDirectoryUrl =
                    ServiceEnvironment.string(
                        "PRESENCE_DIRECTORY_URL",
                        "http://localhost:8091"
                    ),
                federationInternalApiToken =
                    ServiceEnvironment.secret("FEDERATION_INTERNAL_API_TOKEN") ?: legacyToken,
                pushInternalApiToken =
                    ServiceEnvironment.secret("PUSH_INTERNAL_API_TOKEN") ?: legacyToken,
                gatewayInternalApiToken =
                    ServiceEnvironment.secret("GATEWAY_INTERNAL_API_TOKEN") ?: legacyToken,
                maximumFrameBytes =
                    ServiceEnvironment.long("MAX_FRAME_BYTES", DEFAULT_MAXIMUM_FRAME_BYTES),
                routeLifetimeMilliseconds =
                    ServiceEnvironment.long(
                        "ROUTE_LIFETIME_MILLISECONDS",
                        DEFAULT_ROUTE_LIFETIME_MILLISECONDS
                    ),
                routeRefreshIntervalMilliseconds =
                    ServiceEnvironment.long(
                        "ROUTE_REFRESH_INTERVAL_MILLISECONDS",
                        DEFAULT_ROUTE_REFRESH_INTERVAL_MILLISECONDS
                    ),
                blobStoragePath =
                    ServiceEnvironment.string("BLOB_STORAGE_PATH", ".sparrow-server/blobs"),
                maximumBlobBytes =
                    ServiceEnvironment.long("BLOB_MAXIMUM_BYTES", DEFAULT_MAXIMUM_BLOB_BYTES),
                maximumBlobStorageBytes =
                    ServiceEnvironment.long("BLOB_MAXIMUM_STORAGE_BYTES", DEFAULT_MAXIMUM_BLOB_STORAGE_BYTES),
                maximumBlobRetentionMilliseconds =
                    ServiceEnvironment.long(
                        "BLOB_MAXIMUM_RETENTION_MILLISECONDS",
                        DEFAULT_MAXIMUM_BLOB_RETENTION_MILLISECONDS
                    ),
                blobCleanupIntervalMilliseconds =
                    ServiceEnvironment.long(
                        "BLOB_CLEANUP_INTERVAL_MILLISECONDS",
                        DEFAULT_BLOB_CLEANUP_INTERVAL_MILLISECONDS
                    ),
                blobUploadTicketLifetimeMilliseconds =
                    ServiceEnvironment.long(
                        "BLOB_UPLOAD_TICKET_LIFETIME_MILLISECONDS",
                        DEFAULT_BLOB_UPLOAD_TICKET_LIFETIME_MILLISECONDS
                    )
            )
        }

        private fun configuredControlPlaneUrls(): List<String> =
            System.getenv("CONTROL_PLANE_URLS")
                ?.takeIf(String::isNotBlank)
                ?.let { value ->
                    controlPlaneUrlsFromEnvironment(
                        legacyEnvironmentNames = emptyList(),
                        defaultUrl = value
                    )
                }
                ?: emptyList()

        private fun advertisedControlPlaneUrls(fallback: List<String>): List<String> {
            val configured =
                System.getenv("ADVERTISED_CONTROL_PLANE_URLS")
                    ?.takeIf(String::isNotBlank)
                    ?: return fallback
            return configured
                .split(',', ';')
                .map(String::trim)
                .filter(String::isNotBlank)
                .map { value -> value.trimEnd('/') }
                .distinct()
        }

        private const val DEFAULT_MAXIMUM_FRAME_BYTES = 1_048_576L
        private const val DEFAULT_ROUTE_LIFETIME_MILLISECONDS = 90_000L
        private const val DEFAULT_ROUTE_REFRESH_INTERVAL_MILLISECONDS = 30_000L
        private const val DEFAULT_MAXIMUM_BLOB_BYTES = 128L * 1024L * 1024L
        private const val DEFAULT_MAXIMUM_BLOB_STORAGE_BYTES = 10L * 1024L * 1024L * 1024L
        private const val DEFAULT_MAXIMUM_BLOB_RETENTION_MILLISECONDS = 30L * 24L * 60L * 60L * 1_000L
        private const val DEFAULT_BLOB_CLEANUP_INTERVAL_MILLISECONDS = 60L * 60L * 1_000L
        private const val DEFAULT_BLOB_UPLOAD_TICKET_LIFETIME_MILLISECONDS = 5L * 60L * 1_000L
    }
}
