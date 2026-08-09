package com.cbgm.securechat.server.gateway

import com.cbgm.securechat.server.persistence.ServiceEnvironment
import com.cbgm.securechat.server.persistence.controlPlaneUrlsFromEnvironment
import com.cbgm.securechat.server.security.NodeIdentity
import com.cbgm.securechat.server.security.NodeIdentityStore
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
    val pushNodeApiUrl: String?,
    val presenceDirectoryUrl: String,
    val federationInternalApiToken: String?,
    val pushInternalApiToken: String?,
    val gatewayInternalApiToken: String?,
    val maximumFrameBytes: Long,
    val routeLifetimeMilliseconds: Long,
    val routeRefreshIntervalMilliseconds: Long
) {
    init {
        require(routeLifetimeMilliseconds > 0L) {
            "Route lifetime must be positive"
        }
        require(routeRefreshIntervalMilliseconds in 1 until routeLifetimeMilliseconds) {
            "Route refresh interval must be positive and shorter than the route lifetime"
        }
    }

    companion object {
        fun fromEnvironment(): GatewayConfig {
            val legacyToken = ServiceEnvironment.secret("INTERNAL_API_TOKEN")
            return GatewayConfig(
                port = ServiceEnvironment.int("PORT", DEFAULT_GATEWAY_PORT),
                nodeIdentityPath =
                    ServiceEnvironment.string(
                        "NODE_IDENTITY_PATH",
                        ".securechat-server/node.identity"
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
                    )
            )
        }

        private const val DEFAULT_MAXIMUM_FRAME_BYTES = 1_048_576L
        private const val DEFAULT_ROUTE_LIFETIME_MILLISECONDS = 90_000L
        private const val DEFAULT_ROUTE_REFRESH_INTERVAL_MILLISECONDS = 30_000L
    }
}
