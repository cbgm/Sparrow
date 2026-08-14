package com.cbgm.securechat.feature.transport.gateway.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
data class GatewayNodeInformation(
    val nodeId: String,
    val routeLifetimeMilliseconds: Long,
    val routeRefreshIntervalMilliseconds: Long,
    @Transient
    val serverTimeEpochMilliseconds: Long? = null,
    @Transient
    val serverTimeObservedAtEpochMilliseconds: Long? = null
) {
    init {
        require(nodeId.isNotBlank()) { "Gateway node ID must not be blank" }
        require(routeLifetimeMilliseconds > 0L) { "Route lifetime must be positive" }
        require(routeRefreshIntervalMilliseconds in 1 until routeLifetimeMilliseconds) {
            "Route refresh interval must be positive and shorter than the route lifetime"
        }
    }
}

@Serializable
data class ClientRoute(
    val routingId: String,
    val nodeId: String,
    val connectionId: String,
    val generation: Long,
    val expiresAtEpochMilliseconds: Long,
    val aliases: List<String>? = null,
    val clientSignature: ByteArray
)

@Serializable
data class UnsignedClientRoute(
    val routingId: String,
    val nodeId: String,
    val connectionId: String,
    val generation: Long,
    val expiresAtEpochMilliseconds: Long,
    val aliases: List<String>? = null
)

fun ClientRoute.unsigned(): UnsignedClientRoute =
    UnsignedClientRoute(
        routingId = routingId,
        nodeId = nodeId,
        connectionId = connectionId,
        generation = generation,
        expiresAtEpochMilliseconds = expiresAtEpochMilliseconds,
        aliases = aliases
    )

@Serializable
data class ClientRouteRegistration(
    val route: ClientRoute,
    val clientSigningPublicKey: ByteArray
)
