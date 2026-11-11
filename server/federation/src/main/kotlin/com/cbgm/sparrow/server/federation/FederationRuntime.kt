package com.cbgm.sparrow.server.federation

import com.cbgm.sparrow.server.observability.installServerObservability
import com.cbgm.sparrow.server.persistence.ControlPlaneEndpointPool
import com.cbgm.sparrow.server.protocol.serverJson
import com.cbgm.sparrow.server.security.NodeIdentity
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStopped
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.nio.file.Path
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation as ClientContentNegotiation

internal fun createManagedHttpClient(suppliedHttpClient: HttpClient?): ManagedHttpClient =
    if (suppliedHttpClient != null) {
        ManagedHttpClient(suppliedHttpClient, owned = false)
    } else {
        ManagedHttpClient(
            client =
                HttpClient(CIO) {
                    install(ClientContentNegotiation) { json(serverJson) }
                },
            owned = true
        )
    }

internal fun createFederationRuntime(
    config: FederationConfig,
    httpClient: HttpClient
): FederationRuntime {
    // The worker publishes only directory-signature-verified public origins.
    // Configured local/manual planes remain separate and always take priority.
    val discoveryFile = System.getenv("DIRECTORY_DISCOVERY_PATH")
        ?.takeIf(String::isNotBlank)
        ?.let { Path.of(it) }
    return FederationRuntime(
        httpClient = httpClient,
        serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
        registryEndpointPool =
            ControlPlaneEndpointPool(
                config.controlPlaneUrls.ifEmpty { listOf(config.nodeRegistryUrl) },
                discoveryFile = discoveryFile
            )
    )
}

internal fun Application.configureFederationLifecycle(
    runtime: FederationRuntime,
    identity: NodeIdentity,
    config: FederationConfig,
    closeHttpClient: Boolean
) {
    if (config.registerNode) {
        runtime.serviceScope.launch {
            NodeRegistrationAgent(
                httpClient = runtime.httpClient,
                identity = identity,
                config = config.toNodeRegistrationConfig(),
                endpointPool = runtime.registryEndpointPool
            ).run()
        }
    }
    monitor.subscribe(ApplicationStopped) {
        runtime.serviceScope.cancel()
        if (closeHttpClient) {
            runtime.httpClient.close()
        }
    }
}

private fun FederationConfig.toNodeRegistrationConfig(): NodeRegistrationConfig =
    NodeRegistrationConfig(
        clientEndpoint = clientEndpoint,
        federationEndpoint = federationEndpoint,
        mailboxEndpoint = mailboxEndpoint
    )

internal fun Application.installFederationPlugins() {
    installServerObservability("federation") { true }
    install(ContentNegotiation) { json(serverJson) }
}

internal data class ManagedHttpClient(
    val client: HttpClient,
    val owned: Boolean
)

internal data class FederationRuntime(
    val httpClient: HttpClient,
    val serviceScope: CoroutineScope,
    val registryEndpointPool: ControlPlaneEndpointPool
)
