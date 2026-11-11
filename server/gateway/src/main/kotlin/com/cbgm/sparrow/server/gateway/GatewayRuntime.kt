package com.cbgm.sparrow.server.gateway

import com.cbgm.sparrow.server.observability.installServerObservability
import com.cbgm.sparrow.server.protocol.serverJson
import com.cbgm.sparrow.server.security.CreationWorkCheck
import com.cbgm.sparrow.server.security.NodeIdentity
import com.cbgm.sparrow.server.security.relayKeyPair
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

private const val WEBSOCKET_PING_PERIOD_SECONDS = 20
private const val WEBSOCKET_TIMEOUT_SECONDS = 60

/**
 * The node's edge for apps: relays and circuits (the circuit hops are WebSockets), and blobs. Apps hold no session
 * here; messages go into mailboxes through the circuits.
 */
internal data class GatewayRuntime(
    val blobStore: BlobStore,
    val blobCreationWork: CreationWorkCheck,
    val serviceScope: CoroutineScope,
    val relay: OnionRelay,
    val circuits: CircuitRelay
)

internal fun createGatewayRuntime(
    identity: NodeIdentity,
    config: GatewayConfig
): GatewayRuntime {
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

    return GatewayRuntime(
        blobStore = blobStore,
        blobCreationWork = CreationWorkCheck(config.blobCreationWorkBits),
        serviceScope = serviceScope,
        relay =
            OnionRelay(
                keys = identity.relayKeyPair(),
                httpClient = createRelayHttpClient(),
                destinations = RelayDestinationPolicy(allowPrivateAddresses = config.relayPrivateDestinations)
            ),
        circuits =
            CircuitRelay(
                keys = identity.relayKeyPair(),
                httpClient = createCircuitHttpClient(),
                destinations = RelayDestinationPolicy(allowPrivateAddresses = config.relayPrivateDestinations)
            )
    )
}

internal fun Application.configureGatewayLifecycle(runtime: GatewayRuntime) {
    monitor.subscribe(ApplicationStopped) {
        runtime.relay.close()
        runtime.circuits.close()
        runtime.serviceScope.cancel()
    }
}

internal fun Application.installGatewayPlugins(config: GatewayConfig) {
    installServerObservability("gateway") { true }
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
