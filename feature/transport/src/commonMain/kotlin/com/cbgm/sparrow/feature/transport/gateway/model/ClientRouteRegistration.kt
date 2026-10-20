package com.cbgm.sparrow.feature.transport.gateway.model

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
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ClientRoute

        if (generation != other.generation) return false
        if (expiresAtEpochMilliseconds != other.expiresAtEpochMilliseconds) return false
        if (routingId != other.routingId) return false
        if (nodeId != other.nodeId) return false
        if (connectionId != other.connectionId) return false
        if (aliases != other.aliases) return false
        if (!clientSignature.contentEquals(other.clientSignature)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = generation.hashCode()
        result = 31 * result + expiresAtEpochMilliseconds.hashCode()
        result = 31 * result + routingId.hashCode()
        result = 31 * result + nodeId.hashCode()
        result = 31 * result + connectionId.hashCode()
        result = 31 * result + (aliases?.hashCode() ?: 0)
        result = 31 * result + clientSignature.contentHashCode()
        return result
    }
}

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
    val clientSigningPublicKey: ByteArray,
    // One-time proof of the phone number verified at sign-up; only the first registration sends it.
    val phoneVerificationToken: String? = null
) {
    override fun toString(): String = "ClientRouteRegistration(routingId=${route.routingId})"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ClientRouteRegistration

        if (route != other.route) return false
        if (!clientSigningPublicKey.contentEquals(other.clientSigningPublicKey)) return false
        if (phoneVerificationToken != other.phoneVerificationToken) return false

        return true
    }

    override fun hashCode(): Int {
        var result = route.hashCode()
        result = 31 * result + clientSigningPublicKey.contentHashCode()
        result = 31 * result + (phoneVerificationToken?.hashCode() ?: 0)
        return result
    }
}
