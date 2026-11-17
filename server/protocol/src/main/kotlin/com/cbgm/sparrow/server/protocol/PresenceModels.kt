package com.cbgm.sparrow.server.protocol

import kotlinx.serialization.Serializable

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
    /** Where others put packets for this device before they have its mailbox route ([InvitationRouteResult]). */
    val invitationRoute: InvitationRoute? = null,
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
    val invitationRoute: InvitationRoute? = null,
    val issuedAtEpochMilliseconds: Long
) {
    override fun toString(): String = "UnsignedAliasClaim(redacted)"

    override fun equals(other: Any?): Boolean =
        other is UnsignedAliasClaim &&
            routingId == other.routingId &&
            clientSigningPublicKey.contentEquals(other.clientSigningPublicKey) &&
            aliases == other.aliases &&
            phoneAttestation == other.phoneAttestation &&
            invitationRoute == other.invitationRoute &&
            issuedAtEpochMilliseconds == other.issuedAtEpochMilliseconds

    override fun hashCode(): Int =
        listOf(routingId, clientSigningPublicKey.contentHashCode(), aliases, phoneAttestation, invitationRoute, issuedAtEpochMilliseconds)
            .hashCode()
}

fun AliasClaim.unsigned(): UnsignedAliasClaim =
    UnsignedAliasClaim(
        routingId = routingId,
        clientSigningPublicKey = clientSigningPublicKey,
        aliases = aliases,
        phoneAttestation = phoneAttestation,
        invitationRoute = invitationRoute,
        issuedAtEpochMilliseconds = issuedAtEpochMilliseconds
    )

/**
 * A device's invitation mailbox ([CreateMailboxRequest.invitation]) and its raw X25519 [encryptionPublicKey]: senders
 * seal what they put there for that key, so the mailbox's node reads neither the invitation nor who sent it.
 */
@Serializable
data class InvitationRoute(
    val route: DeliveryRoute,
    val encryptionPublicKey: ByteArray
) {
    override fun equals(other: Any?): Boolean =
        other is InvitationRoute && route == other.route && encryptionPublicKey.contentEquals(other.encryptionPublicKey)

    override fun hashCode(): Int = 31 * route.hashCode() + encryptionPublicKey.contentHashCode()
}

/**
 * `POST /v1/invitation-routes` on the presence directory: the invitation route of [routingId] (a phone alias or a
 * device routing ID), paid with [work] (the mailbox proof of work), because anyone may ask and the directory does not
 * know who asks.
 */
@Serializable
data class InvitationRouteLookup(
    val routingId: String,
    val work: MailboxCreationWork
)

/** The device ([ownerRoutingId]; for a phone alias the device holding it) and its [invitationRoute]. */
@Serializable
data class InvitationRouteResult(
    val ownerRoutingId: String,
    val invitationRoute: InvitationRoute
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
