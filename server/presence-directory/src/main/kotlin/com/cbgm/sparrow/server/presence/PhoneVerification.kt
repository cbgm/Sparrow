package com.cbgm.sparrow.server.presence

import com.cbgm.sparrow.server.protocol.ClientRouteRegistration
import com.cbgm.sparrow.server.security.ClientRoutingIds
import kotlinx.coroutines.CancellationException

internal data class VerifiedPhone(
    val phoneNumber: String,
    val expiresAtEpochMilliseconds: Long
)

internal fun interface PhoneTokenVerifier {
    suspend fun verify(token: String): VerifiedPhone
}

/**
 * Phone numbers are verified at most once, in the app during sign-up. Its first route registration
 * carries the Firebase token; later registrations carry none. The token itself is never stored:
 * it only proves one alias for the registration it arrives with (see [AliasClaims]).
 */
internal class PhoneVerification(
    private val verifier: PhoneTokenVerifier?,
    private val testAliases: Set<String> = emptySet()
) {
    /** Returns null when a token was sent but proves nothing; such a registration is refused. */
    suspend fun claims(registration: ClientRouteRegistration): AliasClaims? {
        val aliases = registration.route.aliases.orEmpty()
        val token = registration.phoneVerificationToken
        val unverified =
            AliasClaims(provenAlias = null, unprovenClaimsAllowed = verifier == null, testAliases = testAliases)
        return when {
            aliases.isEmpty() || token == null || verifier == null -> unverified
            token.isBlank() -> null
            else -> verifiedAlias(verifier, token)?.let { alias -> unverified.copy(provenAlias = alias) }
        }
    }

    private suspend fun verifiedAlias(verifier: PhoneTokenVerifier, token: String): String? =
        try {
            ClientRoutingIds.fromPhoneNumber(verifier.verify(token).phoneNumber)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Exception) {
            null
        }
}

/**
 * Which phone aliases a route registration may hold. An alias belongs to the first device that
 * claims it and stays bound to it while that device keeps registering (see
 * [ALIAS_BINDING_LIFETIME_MILLISECONDS]). A free alias needs a phone proof when the server can
 * verify phones; without phone verification the first claim wins. Only a proof moves an alias
 * that is bound to another device. The operator's test numbers ([testAliases]) count as proven for every
 * registration: the device that registers one last holds it, so reinstalled test devices need no SMS.
 */
data class AliasClaims(
    val provenAlias: String?,
    val unprovenClaimsAllowed: Boolean,
    val testAliases: Set<String> = emptySet()
) {
    fun isProven(alias: String): Boolean = alias == provenAlias || alias in testAliases

    fun allowsFreeAlias(alias: String): Boolean = unprovenClaimsAllowed || isProven(alias)

    fun allows(
        alias: String,
        deviceRoutingId: String,
        currentOwner: String?
    ): Boolean =
        when (currentOwner) {
            deviceRoutingId -> true
            null -> allowsFreeAlias(alias)
            else -> isProven(alias)
        }
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
