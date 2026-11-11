package com.cbgm.sparrow.feature.transport.presence

import kotlinx.serialization.Serializable

/**
 * The device's phone aliases (same wire format as `AliasClaim` in `:server:protocol`), signed with its key and sealed
 * for one Control Plane's presence directory. The claims after sign-up carry the phone verifier's attestation
 * ([PhoneAttestationRemoteSource]).
 */
@Serializable
internal class AliasClaim(
    val routingId: String,
    val clientSigningPublicKey: ByteArray,
    val aliases: List<String>,
    val phoneAttestation: String? = null,
    val issuedAtEpochMilliseconds: Long,
    val signature: ByteArray
) {
    override fun toString(): String = "AliasClaim(redacted)"
}

@Serializable
internal class UnsignedAliasClaim(
    val routingId: String,
    val clientSigningPublicKey: ByteArray,
    val aliases: List<String>,
    val phoneAttestation: String? = null,
    val issuedAtEpochMilliseconds: Long
) {
    override fun toString(): String = "UnsignedAliasClaim(redacted)"
}

@Serializable
internal class AliasClaimRequest(
    val sealedClaim: ByteArray
)

@Serializable
internal class AliasClaimResult(
    val acceptedAliasIndexes: List<Int> = emptyList()
)

internal const val ALIAS_CLAIM_CONTEXT = "sparrow-alias-claim-v1"
