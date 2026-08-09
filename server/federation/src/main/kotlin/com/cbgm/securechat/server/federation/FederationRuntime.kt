package com.cbgm.securechat.server.federation

import com.cbgm.securechat.server.observability.installServerObservability
import com.cbgm.securechat.server.persistence.BoundedIdempotencyStore
import com.cbgm.securechat.server.persistence.ControlPlaneEndpointPool
import com.cbgm.securechat.server.protocol.serverJson
import com.cbgm.securechat.server.security.BoundedRateLimiter
import com.cbgm.securechat.server.security.NodeIdentity
import com.cbgm.securechat.server.security.NodeRequestSigner
import com.cbgm.securechat.server.security.NodeRequestVerifier
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStopped
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.forwardedheaders.XForwardedHeaders
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
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
    identity: NodeIdentity,
    config: FederationConfig,
    httpClient: HttpClient
): FederationRuntime {
    val registryEndpointPool =
        ControlPlaneEndpointPool(
            config.controlPlaneUrls.ifEmpty { listOf(config.nodeRegistryUrl) }
        )
    val presenceEndpointPool =
        ControlPlaneEndpointPool(
            config.controlPlaneUrls.ifEmpty { listOf(config.presenceDirectoryUrl) }
        )
    val registry = CachingNodeRegistryClient(httpClient, registryEndpointPool)
    val localGateway =
        HttpLocalGatewayClient(
            httpClient = httpClient,
            baseUrl = config.gatewayInternalUrl,
            internalToken = config.gatewayInternalApiToken
        )
    val remoteFederation =
        HttpRemoteFederationClient(
            httpClient = httpClient,
            signer = NodeRequestSigner(identity)
        )
    val outboundQueue = createOutboundEnvelopeStorage(config)
    val router =
        FederationRouter(
            localNodeId = identity.nodeId,
            presenceDirectory =
                HttpPresenceDirectoryClient(httpClient, presenceEndpointPool),
            nodeRegistry = registry,
            localGateway = localGateway,
            remoteFederation = remoteFederation,
            mailbox = HttpMailboxClient(httpClient),
            localTypingGateway = localGateway,
            remoteTypingFederation = remoteFederation,
            queue = outboundQueue,
            retryBaseDelayMilliseconds = config.outboundRetryBaseDelayMilliseconds,
            retryMaximumDelayMilliseconds = config.outboundRetryMaximumDelayMilliseconds
        )

    return FederationRuntime(
        httpClient = httpClient,
        incomingRateLimiter = BoundedRateLimiter(config.incomingRateLimit),
        registry = registry,
        localGateway = localGateway,
        outboundQueue = outboundQueue,
        router = router,
        verifier = NodeRequestVerifier(),
        incomingIds =
            BoundedIdempotencyStore(
                maximumEntries = config.maximumDeduplicationEntries
            ),
        serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
        registryEndpointPool = registryEndpointPool
    )
}

internal fun Application.configureFederationLifecycle(
    runtime: FederationRuntime,
    identity: NodeIdentity,
    config: FederationConfig,
    closeHttpClient: Boolean
) {
    runtime.serviceScope.launch {
        runtime.registry.runRefreshLoop()
    }
    runtime.serviceScope.launch {
        OutboundEnvelopeRetryAgent(
            router = runtime.router,
            pollIntervalMilliseconds = config.outboundRetryPollIntervalMilliseconds,
            batchSize = config.outboundRetryBatchSize
        ).run()
    }
    if (config.registerNode) {
        runtime.serviceScope.launch {
            NodeRegistrationAgent(
                httpClient = runtime.httpClient,
                identity = identity,
                config = config.toNodeRegistrationConfig(),
                endpointPool = runtime.registryEndpointPool,
                loadProvider =
                    HttpGatewayLoadProvider(
                        httpClient = runtime.httpClient,
                        baseUrl = config.gatewayInternalUrl,
                        internalToken = config.gatewayInternalApiToken
                    )
            ).run()
        }
    }
    monitor.subscribe(ApplicationStopped) {
        runtime.serviceScope.cancel()
        runtime.outboundQueue.close()
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

internal fun Application.installFederationPlugins(
    runtime: FederationRuntime,
    config: FederationConfig
) {
    installServerObservability("federation") {
        runtime.router.pendingCount()
        true
    }
    install(ContentNegotiation) { json(serverJson) }
    if (config.trustProxyHeaders) {
        install(XForwardedHeaders)
    }
}

internal data class ManagedHttpClient(
    val client: HttpClient,
    val owned: Boolean
)

internal data class FederationRuntime(
    val httpClient: HttpClient,
    val incomingRateLimiter: BoundedRateLimiter,
    val registry: CachingNodeRegistryClient,
    val localGateway: LocalGatewayClient,
    val outboundQueue: OutboundEnvelopeStorage,
    val router: FederationRouter,
    val verifier: NodeRequestVerifier,
    val incomingIds: BoundedIdempotencyStore,
    val serviceScope: CoroutineScope,
    val registryEndpointPool: ControlPlaneEndpointPool
)
