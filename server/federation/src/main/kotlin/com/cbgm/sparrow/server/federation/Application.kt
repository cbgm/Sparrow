package com.cbgm.sparrow.server.federation

import com.cbgm.sparrow.server.persistence.ServiceEnvironment
import com.cbgm.sparrow.server.persistence.controlPlaneUrlsFromEnvironment
import com.cbgm.sparrow.server.security.NodeIdentity
import com.cbgm.sparrow.server.security.NodeIdentityStore
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
                    ".sparrow-server/node.identity"
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

/**
 * Registers this node at its Control Planes and keeps its signed descriptor fresh, so apps find its relays, circuits,
 * mailboxes and blobs in the node directory. Nodes no longer pass envelopes to each other: apps put them into
 * mailboxes themselves, through their circuits.
 */
fun Application.federationModule(
    identity: NodeIdentity,
    config: FederationConfig = FederationConfig.fromEnvironment(),
    suppliedHttpClient: HttpClient? = null
) {
    val managedHttpClient = createManagedHttpClient(suppliedHttpClient)
    val runtime = createFederationRuntime(config, managedHttpClient.client)

    configureFederationLifecycle(runtime, identity, config, managedHttpClient.owned)
    installFederationPlugins()
    installFederationRoutes()
}

data class FederationConfig(
    val controlPlaneUrls: List<String>,
    val nodeRegistryUrl: String,
    val registerNode: Boolean,
    val clientEndpoint: String,
    val federationEndpoint: String,
    val mailboxEndpoint: String
) {
    companion object {
        fun fromEnvironment(): FederationConfig =
            with(ServiceEnvironment) {
                FederationConfig(
                    controlPlaneUrls =
                        optionalEnv("CONTROL_PLANE_URLS")
                            ?.let { controlPlaneUrlsFromEnvironment(emptyList(), it) }
                            ?: emptyList(),
                    nodeRegistryUrl = string("NODE_REGISTRY_URL", "http://localhost:8090"),
                    registerNode = boolean("REGISTER_NODE", true),
                    clientEndpoint = string("CLIENT_ENDPOINT", "ws://localhost:8094/v1/gateway"),
                    federationEndpoint = string("FEDERATION_ENDPOINT", "http://localhost:8093"),
                    mailboxEndpoint = string("MAILBOX_ENDPOINT", "http://localhost:8092")
                )
            }
    }
}

internal fun ServiceEnvironment.boolean(key: String, default: Boolean): Boolean =
    string(key, default.toString()).toBoolean()

internal fun ServiceEnvironment.optionalEnv(key: String): String? =
    System.getenv(key)?.takeIf(String::isNotBlank)
