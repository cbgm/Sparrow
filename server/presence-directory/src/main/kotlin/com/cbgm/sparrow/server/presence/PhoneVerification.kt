package com.cbgm.sparrow.server.presence

import com.cbgm.sparrow.server.protocol.UnsignedAliasClaim
import com.cbgm.sparrow.server.security.ClientRoutingIds

/**
 * Phone numbers are verified once, by the phone verifier next to the Control Plane Directory, never here: an alias
 * claim proves its phone alias with that verifier's attestation ([PhoneAttestations]). A claim without one proves
 * nothing; an attestation that does not check out refuses the registration.
 */
internal class PhoneVerification(
    private val attestations: PhoneAttestations,
    private val testAliases: Set<String> = emptySet()
) {
    /** Returns null when an attestation was sent but proves nothing; such a registration is refused. */
    fun claims(claim: UnsignedAliasClaim?): AliasClaims? {
        val aliases = claim?.aliases.orEmpty()
        val attestation = claim?.phoneAttestation
        val unverified = AliasClaims(provenAlias = null, testAliases = testAliases, aliases = aliases)
        return when {
            aliases.isEmpty() || attestation == null -> unverified
            else -> attestations.provenAlias(attestation, claim.routingId)?.let { alias -> unverified.copy(provenAlias = alias) }
        }
    }
}

internal fun createPhoneVerification(environment: (String) -> String? = System::getenv): PhoneVerification =
    PhoneVerification(
        attestations = PhoneAttestations(directoryPublicKey(environment("CONTROL_PLANE_DIRECTORY_PUBLIC_KEY"))),
        testAliases = testPhoneAliases(environment("PRESENCE_TEST_PHONE_ALIASES"))
    )

/**
 * Which of the claimed phone [aliases] a route registration may hold. An alias belongs to the first device that
 * proves it and stays bound to it while that device keeps registering (see [ALIAS_BINDING_LIFETIME_MILLISECONDS]).
 * A free alias needs a phone proof, and only a proof moves an alias that is bound to another device. The operator's
 * test numbers ([testAliases]) count as proven for every registration: the device that registers one last holds it,
 * so reinstalled test devices need no SMS.
 */
data class AliasClaims(
    val provenAlias: String?,
    val testAliases: Set<String> = emptySet(),
    val aliases: List<String> = emptyList()
) {
    fun isProven(alias: String): Boolean = alias == provenAlias || alias in testAliases

    fun allows(
        alias: String,
        deviceRoutingId: String,
        currentOwner: String?
    ): Boolean = currentOwner == deviceRoutingId || isProven(alias)
}

/** Reads the operator's test numbers as phone aliases (`scphone1_...`, comma-separated); never raw numbers. */
internal fun testPhoneAliases(value: String?): Set<String> =
    value.orEmpty()
        .split(',')
        .map(String::trim)
        .filter(String::isNotEmpty)
        .onEach { alias -> require(ClientRoutingIds.isBootstrapRoutingId(alias)) { "Invalid test phone alias" } }
        .toSet()

/** An alias binding is renewed by every registration and forgotten this long after the last one. */
internal const val ALIAS_BINDING_LIFETIME_MILLISECONDS = 30L * 24L * 60L * 60L * 1_000L
