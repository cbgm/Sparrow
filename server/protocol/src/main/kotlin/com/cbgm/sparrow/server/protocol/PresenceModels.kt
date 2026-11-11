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

/** Routes carry no phone aliases; the app claims them directly at each Control Plane ([AliasClaim]). */
@Serializable
data class ClientRouteRegistration(
    val route: ClientRoute,
    val clientSigningPublicKey: ByteArray
) {
    override fun toString(): String = "ClientRouteRegistration(redacted)"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ClientRouteRegistration

        if (route != other.route) return false
        if (!clientSigningPublicKey.contentEquals(other.clientSigningPublicKey)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = route.hashCode()
        result = 31 * result + clientSigningPublicKey.contentHashCode()
        return result
    }
}

/**
 * The phone aliases a device holds, with the phone verifier's attestation (`phone_verifier.py` next to the Control
 * Plane Directory) in the claims right after sign-up.
 * Signed with the device key ([clientSigningPublicKey] derives [routingId]) and sealed for one Control Plane's presence
 * directory ([NodeDirectory.aliasClaimKey]). The app sends it through two relays (see [RelayLayer]), so neither the
 * relays nor the gateway read it and the Control Plane does not see the app's address.
 */
@Serializable
data class AliasClaim(
    val routingId: String,
    val clientSigningPublicKey: ByteArray,
    val aliases: List<String>,
    val phoneAttestation: String? = null,
    val issuedAtEpochMilliseconds: Long,
    val signature: ByteArray
) {
    override fun toString(): String = "AliasClaim(redacted)"

    override fun equals(other: Any?): Boolean =
        other is AliasClaim && unsigned() == other.unsigned() && signature.contentEquals(other.signature)

    override fun hashCode(): Int = 31 * unsigned().hashCode() + signature.contentHashCode()
}

@Serializable
data class UnsignedAliasClaim(
    val routingId: String,
    val clientSigningPublicKey: ByteArray,
    val aliases: List<String>,
    val phoneAttestation: String? = null,
    val issuedAtEpochMilliseconds: Long
) {
    override fun toString(): String = "UnsignedAliasClaim(redacted)"

    override fun equals(other: Any?): Boolean =
        other is UnsignedAliasClaim &&
            routingId == other.routingId &&
            clientSigningPublicKey.contentEquals(other.clientSigningPublicKey) &&
            aliases == other.aliases &&
            phoneAttestation == other.phoneAttestation &&
            issuedAtEpochMilliseconds == other.issuedAtEpochMilliseconds

    override fun hashCode(): Int =
        listOf(routingId, clientSigningPublicKey.contentHashCode(), aliases, phoneAttestation, issuedAtEpochMilliseconds)
            .hashCode()
}

fun AliasClaim.unsigned(): UnsignedAliasClaim =
    UnsignedAliasClaim(
        routingId = routingId,
        clientSigningPublicKey = clientSigningPublicKey,
        aliases = aliases,
        phoneAttestation = phoneAttestation,
        issuedAtEpochMilliseconds = issuedAtEpochMilliseconds
    )

/** The presence directory's raw X25519 key for [AliasClaim]s; see [NodeDirectory.aliasClaimKey]. */
@Serializable
data class AliasClaimKeyResponse(
    val publicKey: ByteArray
) {
    override fun equals(other: Any?): Boolean = other is AliasClaimKeyResponse && publicKey.contentEquals(other.publicKey)

    override fun hashCode(): Int = publicKey.contentHashCode()
}

/** `POST /v1/alias-claims` on the presence directory: an [AliasClaim] sealed for its key. */
@Serializable
data class AliasClaimRequest(
    val sealedClaim: ByteArray
) {
    override fun equals(other: Any?): Boolean = other is AliasClaimRequest && sealedClaim.contentEquals(other.sealedClaim)

    override fun hashCode(): Int = sealedClaim.contentHashCode()
}

/**
 * Positions in [AliasClaim.aliases] the presence directory bound to the device; the others are held by another
 * device. Positions keep the aliases out of the reply, which the exit relay passes on.
 */
@Serializable
data class AliasClaimResult(
    val acceptedAliasIndexes: List<Int> = emptyList()
)

/** For a phone alias, [ownerRoutingId] is the device holding it; [routes] are that device's routes here, if any. */
@Serializable
data class ClientRoutingResult(
    val routingId: String,
    val routes: List<ClientRoute>,
    val ownerRoutingId: String? = null
)
