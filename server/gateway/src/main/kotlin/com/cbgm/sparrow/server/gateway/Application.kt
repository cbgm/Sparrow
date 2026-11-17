package com.cbgm.sparrow.server.gateway

import com.cbgm.sparrow.server.exchangerates.DEFAULT_EXCHANGE_RATES_URL
import com.cbgm.sparrow.server.exchangerates.installExchangeRateRoutes
import com.cbgm.sparrow.server.linkpreview.installLinkPreviewRoutes
import com.cbgm.sparrow.server.persistence.ServiceEnvironment
import com.cbgm.sparrow.server.persistence.controlPlaneUrlsFromEnvironment
import com.cbgm.sparrow.server.security.NodeIdentity
import com.cbgm.sparrow.server.security.NodeIdentityStore
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
    config: GatewayConfig = GatewayConfig.fromEnvironment()
) {
    val runtime = createGatewayRuntime(identity, config)

    configureGatewayLifecycle(runtime)
    installGatewayPlugins(config)
    installGatewayRoutes(runtime, identity, config)
    installLinkPreviewRoutes()
    installExchangeRateRoutes(
        ServiceEnvironment.string("EXCHANGE_RATES_URL", DEFAULT_EXCHANGE_RATES_URL)
    )
}

data class GatewayConfig(
    val port: Int,
    val nodeIdentityPath: String,
    /** The Control Planes this node tells apps about (`/v1/control-planes`). */
    val advertisedControlPlaneUrls: List<String>,
    /** Largest WebSocket frame of a circuit hop. */
    val maximumFrameBytes: Long,
    val blobStoragePath: String,
    val maximumBlobBytes: Long,
    val maximumBlobStorageBytes: Long,
    val maximumBlobRetentionMilliseconds: Long,
    val blobCleanupIntervalMilliseconds: Long,
    /** Lets the relay reach LAN addresses; only for LAN installations, where Control Planes have private addresses. */
    val relayPrivateDestinations: Boolean = false,
    /** Zero bits the proof of work for a blob opened through a circuit needs; the app solves 18 bits. */
    val blobCreationWorkBits: Int = DEFAULT_BLOB_CREATION_WORK_BITS
) {
    init {
        require(blobStoragePath.isNotBlank()) { "Blob storage path must not be blank" }
        require(maximumBlobBytes > 0L) { "Maximum blob size must be positive" }
        require(maximumBlobStorageBytes >= maximumBlobBytes) {
            "Maximum blob storage must hold at least one maximum-size blob"
        }
        require(maximumBlobRetentionMilliseconds > 0L) { "Maximum blob retention must be positive" }
        require(blobCleanupIntervalMilliseconds > 0L) { "Blob cleanup interval must be positive" }
    }

    companion object {
        fun fromEnvironment(): GatewayConfig {
            val controlPlaneUrls = configuredControlPlaneUrls()
            return GatewayConfig(
                port = ServiceEnvironment.int("PORT", DEFAULT_GATEWAY_PORT),
                nodeIdentityPath =
                    ServiceEnvironment.string(
                        "NODE_IDENTITY_PATH",
                        ".sparrow-server/node.identity"
                    ),
                advertisedControlPlaneUrls =
                    advertisedControlPlaneUrls(fallback = controlPlaneUrls),
                maximumFrameBytes =
                    ServiceEnvironment.long("MAX_FRAME_BYTES", DEFAULT_MAXIMUM_FRAME_BYTES),
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
                relayPrivateDestinations = ServiceEnvironment.string("RELAY_PRIVATE_DESTINATIONS", "false") == "true",
                blobCreationWorkBits = ServiceEnvironment.int("BLOB_CREATION_WORK_BITS", DEFAULT_BLOB_CREATION_WORK_BITS)
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
        private const val DEFAULT_MAXIMUM_BLOB_BYTES = 128L * 1024L * 1024L
        private const val DEFAULT_MAXIMUM_BLOB_STORAGE_BYTES = 10L * 1024L * 1024L * 1024L
        private const val DEFAULT_MAXIMUM_BLOB_RETENTION_MILLISECONDS = 30L * 24L * 60L * 60L * 1_000L
        private const val DEFAULT_BLOB_CLEANUP_INTERVAL_MILLISECONDS = 60L * 60L * 1_000L
        private const val DEFAULT_BLOB_CREATION_WORK_BITS = 18
    }
}
