package com.cbgm.sparrow.feature.transport.data.controlplane

import kotlinx.serialization.Serializable

/**
 * The device's phone aliases (same wire format as `AliasClaim` in `:server:protocol`), signed with its key and sealed
 * for one Control Plane's presence directory. The claims after sign-up carry the phone verifier's attestation
 * ([PhoneAttestationRemoteDataSource]); every claim names the device's invitation mailbox ([InvitationRouteDto]).
 */
@Serializable
internal class AliasClaimDto(
    val routingId: String,
    @Suppress("unused") val clientSigningPublicKey: ByteArray,
    val aliases: List<String>,
    val phoneAttestation: String? = null,
    val invitationRoute: InvitationRouteDto? = null,
    val issuedAtEpochMilliseconds: Long,
    val signature: ByteArray
) {
    override fun toString(): String = "AliasClaimDto(redacted)"
}

@Serializable
internal class UnsignedAliasClaimDto(
    val routingId: String,
    val clientSigningPublicKey: ByteArray,
    val aliases: List<String>,
    val phoneAttestation: String? = null,
    val invitationRoute: InvitationRouteDto? = null,
    val issuedAtEpochMilliseconds: Long
) {
    override fun toString(): String = "UnsignedAliasClaimDto(redacted)"
}

@Serializable
internal class AliasClaimRequestDto(
    val sealedClaim: ByteArray
)

@Serializable
internal class AliasClaimResultDto(
    val acceptedAliasIndexes: List<Int> = emptyList()
)

internal const val ALIAS_CLAIM_CONTEXT = "sparrow-alias-claim-v1"
