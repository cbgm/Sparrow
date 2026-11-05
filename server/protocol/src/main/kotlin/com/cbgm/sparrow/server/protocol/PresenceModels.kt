package com.cbgm.sparrow.server.protocol

import kotlinx.serialization.Serializable

@Serializable
data class GatewayNodeInformation(
    val nodeId: String,
    val routeLifetimeMilliseconds: Long,
    val routeRefreshIntervalMilliseconds: Long
) {
    init {
        require(nodeId.isNotBlank())
        require(routeLifetimeMilliseconds > 0L)
        require(routeRefreshIntervalMilliseconds in 1 until routeLifetimeMilliseconds)
    }
}

@Serializable
data class GatewayLoad(
    val activeConnections: Int
) {
    init {
        require(activeConnections >= 0)
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
    init {
        require(routingId.isNotBlank())
        require(nodeId.isNotBlank())
        require(connectionId.isNotBlank())
        require(generation >= 0L)
        require(expiresAtEpochMilliseconds > 0L)
    }

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

/**
 * [sealedAliasClaim] is an [AliasClaim] encrypted for the presence directory (see `AliasClaimKeys`), so the gateway
 * that forwards the registration learns neither the phone aliases nor the phone verification token.
 */
@Serializable
data class ClientRouteRegistration(
    val route: ClientRoute,
    val clientSigningPublicKey: ByteArray,
    val sealedAliasClaim: ByteArray? = null
) {
    override fun toString(): String = "ClientRouteRegistration(redacted)"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ClientRouteRegistration

        if (route != other.route) return false
        if (!clientSigningPublicKey.contentEquals(other.clientSigningPublicKey)) return false
        if (!sealedAliasClaim.contentEquals(other.sealedAliasClaim)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = route.hashCode()
        result = 31 * result + clientSigningPublicKey.contentHashCode()
        result = 31 * result + (sealedAliasClaim?.contentHashCode() ?: 0)
        return result
    }
}

/**
 * The phone aliases of the [AliasClaim] the presence directory stored the route under, as positions in
 * [AliasClaim.aliases]; the others were refused. Positions keep the aliases themselves from the gateway.
 */
@Serializable
data class ClientRouteRegistrationResult(
    val acceptedAliasIndexes: List<Int> = emptyList()
)

/**
 * The phone aliases a device holds, with the one-time phone verification token from sign-up. Signed with the
 * device key and bound to one route, so neither a gateway nor anyone else can claim aliases for a device.
 */
@Serializable
data class AliasClaim(
    val routingId: String,
    val connectionId: String,
    val generation: Long,
    val aliases: List<String>,
    val phoneVerificationToken: String? = null,
    val signature: ByteArray
) {
    override fun toString(): String = "AliasClaim(redacted)"

    override fun equals(other: Any?): Boolean =
        other is AliasClaim && unsigned() == other.unsigned() && signature.contentEquals(other.signature)

    override fun hashCode(): Int = 31 * unsigned().hashCode() + signature.contentHashCode()
}

/** The presence directory's raw X25519 key for [AliasClaim]s; see [NodeDirectory.aliasClaimKey]. */
@Serializable
data class AliasClaimKeyResponse(
    val publicKey: ByteArray
) {
    override fun equals(other: Any?): Boolean = other is AliasClaimKeyResponse && publicKey.contentEquals(other.publicKey)

    override fun hashCode(): Int = publicKey.contentHashCode()
}

@Serializable
data class UnsignedAliasClaim(
    val routingId: String,
    val connectionId: String,
    val generation: Long,
    val aliases: List<String>,
    val phoneVerificationToken: String? = null
) {
    override fun toString(): String = "UnsignedAliasClaim(redacted)"
}

fun AliasClaim.unsigned(): UnsignedAliasClaim =
    UnsignedAliasClaim(
        routingId = routingId,
        connectionId = connectionId,
        generation = generation,
        aliases = aliases,
        phoneVerificationToken = phoneVerificationToken
    )

@Serializable
data class ClientRoutingResult(
    val routingId: String,
    val routes: List<ClientRoute>
)
