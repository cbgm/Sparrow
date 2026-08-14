package com.cbgm.securechat.server.push

import com.cbgm.securechat.server.observability.installServerObservability
import com.cbgm.securechat.server.protocol.ClientRoutingResult
import com.cbgm.securechat.server.protocol.SecureChatNodeDescriptor
import com.cbgm.securechat.server.protocol.serverJson
import com.cbgm.securechat.server.security.BoundedRateLimiter
import com.cbgm.securechat.server.security.NodeDescriptorResolver
import com.cbgm.securechat.server.security.NodeRequestAuthorizer
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
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
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation as ClientContentNegotiation

internal data class PushRuntime(
    val stores: PushStores,
    val devices: PushDeviceStore,
    val pendingEnvelopes: PendingEnvelopeStore,
    val wakeUps: WakeUpStore,
    val coordinator: PushCoordinator,
    val scope: CoroutineScope,
    val deviceRegistrationRateLimiter: BoundedRateLimiter,
    val fcmEnabled: Boolean,
    val nodeApiHttpClient: HttpClient?,
    val nodeRequestAuthorizer: NodeRequestAuthorizer?,
    val nodeRouteOwnershipResolver: NodeRouteOwnershipResolver?
)

internal fun interface NodeRouteOwnershipResolver {
    suspend fun isOwnedBy(
        routingId: String,
        nodeId: String
    ): Boolean
}

internal fun createPushRuntime(config: PushConfig): PushRuntime {
    val stores = createPushStores(config)
    val messaging = FirebasePushSender.createMessagingOrNull()
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    val coordinator = createPushCoordinator(stores, messaging, scope)
    val nodeApi = createNodeApiRuntime(config.nodeRegistryUrl, config.presenceDirectoryUrl)
    coordinator.resumePendingNotifications()

    return PushRuntime(
        stores = stores,
        devices = stores.devices,
        pendingEnvelopes = stores.pendingEnvelopes,
        wakeUps = stores.wakeUps,
        coordinator = coordinator,
        scope = scope,
        deviceRegistrationRateLimiter =
            BoundedRateLimiter(config.deviceRegistrationRateLimit),
        fcmEnabled = messaging != null,
        nodeApiHttpClient = nodeApi?.httpClient,
        nodeRequestAuthorizer = nodeApi?.authorizer,
        nodeRouteOwnershipResolver = nodeApi?.routeOwnershipResolver
    )
}

private fun createPushStores(config: PushConfig): PushStores =
    if (config.databaseUrl == null) {
        PushStores.inMemory(config)
    } else {
        createPostgresPushStores(config)
    }

private fun createPushCoordinator(
    stores: PushStores,
    messaging: com.google.firebase.messaging.FirebaseMessaging?,
    scope: CoroutineScope
): PushCoordinator =
    PushCoordinator(
        pendingEnvelopes = stores.pendingEnvelopes,
        sender =
            FirebasePushSender(
                messaging = messaging,
                devices = stores.devices,
                wakeUps = stores.wakeUps
            ),
        scope = scope
    )

private fun createNodeApiRuntime(
    registryUrl: String?,
    presenceDirectoryUrl: String?
): PushNodeApiRuntime? {
    if (registryUrl == null || presenceDirectoryUrl == null) {
        return null
    }

    val httpClient = createNodeApiHttpClient()
    val descriptorResolver =
        NodeDescriptorResolver { nodeId ->
            fetchNodeDescriptor(httpClient, registryUrl, nodeId)
        }
    val routeOwnershipResolver =
        NodeRouteOwnershipResolver { routingId, nodeId ->
            fetchRouteOwnership(httpClient, presenceDirectoryUrl, routingId, nodeId)
        }
    return PushNodeApiRuntime(
        httpClient = httpClient,
        authorizer = NodeRequestAuthorizer(descriptorResolver),
        routeOwnershipResolver = routeOwnershipResolver
    )
}

private fun createNodeApiHttpClient(): HttpClient =
    HttpClient(CIO) {
        install(ClientContentNegotiation) {
            json(serverJson)
        }
    }

private suspend fun fetchNodeDescriptor(
    httpClient: HttpClient,
    registryUrl: String,
    nodeId: String
): SecureChatNodeDescriptor? {
    val response =
        httpClient.get(
            "${registryUrl.trimEnd('/')}/v1/nodes/$nodeId"
        )
    return if (response.status == HttpStatusCode.OK) {
        response.body<SecureChatNodeDescriptor>()
    } else {
        null
    }
}

private suspend fun fetchRouteOwnership(
    httpClient: HttpClient,
    presenceDirectoryUrl: String,
    routingId: String,
    nodeId: String
): Boolean {
    val response =
        httpClient.get(
            "${presenceDirectoryUrl.trimEnd('/')}/v1/routes/$routingId"
        )
    return response.status == HttpStatusCode.OK &&
        response.body<ClientRoutingResult>().routes.any { route -> route.nodeId == nodeId }
}

internal fun Application.configurePushLifecycle(runtime: PushRuntime) {
    monitor.subscribe(ApplicationStopped) {
        runtime.scope.cancel()
        runtime.nodeApiHttpClient?.close()
        runtime.stores.close()
    }
}

internal fun Application.installPushPlugins(
    runtime: PushRuntime,
    config: PushConfig
) {
    installServerObservability("push") {
        runtime.devices.count()
        runtime.pendingEnvelopes.count()
        true
    }
    install(ContentNegotiation) {
        json(serverJson)
    }
    if (config.trustProxyHeaders) {
        install(XForwardedHeaders)
    }
}

private data class PushNodeApiRuntime(
    val httpClient: HttpClient,
    val authorizer: NodeRequestAuthorizer,
    val routeOwnershipResolver: NodeRouteOwnershipResolver
)
