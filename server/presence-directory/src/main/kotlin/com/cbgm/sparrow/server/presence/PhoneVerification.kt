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
 * Phone numbers are verified once in the app during sign-up. Its first route registration
 * carries the Firebase token; later registrations carry none. Nothing is stored: a token that
 * is present must prove every advertised phone alias, a missing token is accepted.
 */
internal class PhoneVerification(
    private val verifier: PhoneTokenVerifier?
) {
    suspend fun accepts(registration: ClientRouteRegistration): Boolean {
        val aliases = registration.route.aliases.orEmpty()
        val token = registration.phoneVerificationToken
        return when {
            aliases.isEmpty() || token == null || verifier == null -> true
            token.isBlank() -> false
            else -> {
                val alias = verifiedAlias(verifier, token)
                alias != null && aliases.all { it == alias }
            }
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
