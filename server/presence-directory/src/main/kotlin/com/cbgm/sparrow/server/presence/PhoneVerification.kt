package com.cbgm.sparrow.server.presence

import com.cbgm.sparrow.server.protocol.ClientRouteRegistration
import com.cbgm.sparrow.server.security.ClientRoutingIds
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.Serializable

@Serializable
internal data class PhoneVerificationPolicy(
    val required: Boolean,
    val available: Boolean
)

internal data class VerifiedPhone(
    val phoneNumber: String,
    val expiresAtEpochMilliseconds: Long
)

internal fun interface PhoneTokenVerifier {
    suspend fun verify(token: String): VerifiedPhone
}

internal class PhoneVerification(
    private val required: Boolean,
    private val verifier: PhoneTokenVerifier?
) {
    val policy: PhoneVerificationPolicy
        get() = PhoneVerificationPolicy(required = required, available = verifier != null)

    suspend fun accepts(registration: ClientRouteRegistration): Boolean {
        val aliases = registration.route.aliases.orEmpty()
        val token = registration.phoneVerificationToken
        return when {
            aliases.isEmpty() -> true
            verifier == null || token == null -> !required
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
