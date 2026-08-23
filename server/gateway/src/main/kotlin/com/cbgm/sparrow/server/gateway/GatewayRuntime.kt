package com.cbgm.sparrow.server.gateway

import com.cbgm.sparrow.server.observability.installServerObservability
import com.cbgm.sparrow.server.persistence.ControlPlaneEndpointPool
import com.cbgm.sparrow.server.protocol.serverJson
import com.cbgm.sparrow.server.security.NodeIdentity
import com.cbgm.sparrow.server.security.NodeRequestSigner
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStopped
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.websocket.WebSockets
import io.ktor.server.websocket.pingPeriod
import io.ktor.server.websocket.timeout
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.nio.file.Path
import kotlin.time.Duration.Companion.seconds
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation as ClientContentNegotiation

private const val WEBSOCKET_PING_PERIOD_SECONDS = 20
private const val WEBSOCKET_TIMEOUT_SECONDS = 60

internal data class GatewayRuntime(
    val httpClient: HttpClient,
    val ownsHttpClient: Boolean,
    val connections: ConnectionRegistry,
    val handler: GatewayWebSocketHandler,
    val blobStore: BlobStore,
    val blobUploadPermitStore: BlobUploadPermitStore,
    val serviceScope: CoroutineScope
)

internal fun createGatewayRuntime(
    identity: NodeIdentity,
    config: GatewayConfig,
    suppliedHttpClient: HttpClient?
): GatewayRuntime {
    val httpClient = suppliedHttpClient ?: createGatewayHttpClient()
    val connections = ConnectionRegistry()
    val permitStore = BlobUploadPermitStore()
    val blobStore =
        BlobStore(
            root = Path.of(config.blobStoragePath),
            maximumBlobBytes = config.maximumBlobBytes,
            maximumStorageBytes = config.maximumBlobStorageBytes
        )
    val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    serviceScope.launch {
        BlobCleanupAgent(
            store = blobStore,
            cleanupIntervalMilliseconds = config.blobCleanupIntervalMilliseconds
        ).run()
    }
    serviceScope.launch {
        BlobUploadPermitCleanupAgent(
            permitStore = permitStore,
            cleanupIntervalMilliseconds = config.blobCleanupIntervalMilliseconds
        ).run()
    }

    return GatewayRuntime(
        httpClient = httpClient,
        ownsHttpClient = suppliedHttpClient == null,
        connections = connections,
        handler = createGatewayHandler(identity, config, httpClient, connections, permitStore),
        blobStore = blobStore,
        blobUploadPermitStore = permitStore,
        serviceScope = serviceScope
    )
}

private fun createGatewayHttpClient(): HttpClient =
    HttpClient(CIO) {
        install(ClientContentNegotiation) {
            json(serverJson)
        }
    }

private fun createGatewayHandler(
    identity: NodeIdentity,
    config: GatewayConfig,
    httpClient: HttpClient,
    connections: ConnectionRegistry,
    blobUploadPermitStore: BlobUploadPermitStore
): GatewayWebSocketHandler {
    val signer = NodeRequestSigner(identity)
    val presenceEndpointPool =
        ControlPlaneEndpointPool(
            config.controlPlaneUrls.ifEmpty { listOf(config.presenceDirectoryUrl) }
        )
    val pushClient =
        config.pushNodeApiUrl?.let { nodeApiUrl ->
            HttpNodePushClient(
                httpClient = httpClient,
                endpointPool =
                    ControlPlaneEndpointPool(
                        config.controlPlaneUrls.ifEmpty { listOf(nodeApiUrl) }
                    ),
                signer = signer
            )
        } ?: HttpLegacyPushClient(
            httpClient = httpClient,
            baseUrl = config.pushInternalUrl,
            internalToken = config.pushInternalApiToken
        )

    return GatewayWebSocketHandler(
        nodeId = identity.nodeId,
        connections = connections,
        federation =
            HttpFederationClient(
                httpClient = httpClient,
                baseUrl = config.federationInternalUrl,
                internalToken = config.federationInternalApiToken
            ),
        presence =
            HttpPresenceClient(
                httpClient = httpClient,
                endpointPool = presenceEndpointPool,
                signer = signer
            ),
        legacyPush = pushClient,
        routeLifetimeMilliseconds = config.routeLifetimeMilliseconds,
        blobUploadTicketIssuer =
            GatewayBlobUploadTicketIssuer(
                nodeId = identity.nodeId,
                permitStore = blobUploadPermitStore,
                maximumBlobBytes = config.maximumBlobBytes,
                maximumRetentionMilliseconds = config.maximumBlobRetentionMilliseconds,
                ticketLifetimeMilliseconds = config.blobUploadTicketLifetimeMilliseconds
            )
    )
}

internal fun Application.configureGatewayLifecycle(runtime: GatewayRuntime) {
    monitor.subscribe(ApplicationStopped) {
        runtime.handler.close()
        runtime.serviceScope.cancel()
        if (runtime.ownsHttpClient) {
            runtime.httpClient.close()
        }
    }
}

internal fun Application.installGatewayPlugins(
    runtime: GatewayRuntime,
    config: GatewayConfig
) {
    installServerObservability("gateway") {
        runtime.connections.count()
        true
    }
    install(ContentNegotiation) {
        json(serverJson)
    }
    install(WebSockets) {
        pingPeriod = WEBSOCKET_PING_PERIOD_SECONDS.seconds
        timeout = WEBSOCKET_TIMEOUT_SECONDS.seconds
        maxFrameSize = config.maximumFrameBytes
        masking = false
    }
}
