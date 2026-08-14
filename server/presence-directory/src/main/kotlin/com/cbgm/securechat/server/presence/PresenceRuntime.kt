package com.cbgm.securechat.server.presence

import com.cbgm.securechat.server.observability.installServerObservability
import com.cbgm.securechat.server.protocol.SecureChatNodeDescriptor
import com.cbgm.securechat.server.protocol.serverJson
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
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation as ClientContentNegotiation

internal data class PresenceRuntime(
    val store: PresenceStorage,
    val httpClient: HttpClient,
    val registryUrl: String,
    val nodeRequestAuthorizer: NodeRequestAuthorizer
)

internal fun createPresenceRuntime(store: PresenceStorage): PresenceRuntime {
    val httpClient = createPresenceHttpClient()
    val registryUrl =
        System.getenv("NODE_REGISTRY_URL")
            ?.takeIf(String::isNotBlank)
            ?: DEFAULT_NODE_REGISTRY_URL
    val descriptorResolver =
        NodeDescriptorResolver { nodeId ->
            fetchNodeDescriptor(httpClient, registryUrl, nodeId)
        }

    return PresenceRuntime(
        store = store,
        httpClient = httpClient,
        registryUrl = registryUrl,
        nodeRequestAuthorizer = NodeRequestAuthorizer(descriptorResolver)
    )
}

private fun createPresenceHttpClient(): HttpClient =
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

internal fun Application.configurePresenceLifecycle(runtime: PresenceRuntime) {
    monitor.subscribe(ApplicationStopped) {
        runtime.httpClient.close()
        runtime.store.close()
    }
}

internal fun Application.installPresencePlugins(runtime: PresenceRuntime) {
    installServerObservability("presence-directory") {
        runtime.store.routeCount()
        true
    }
    install(ContentNegotiation) {
        json(serverJson)
    }
}

private const val DEFAULT_NODE_REGISTRY_URL = "http://localhost:8090"
