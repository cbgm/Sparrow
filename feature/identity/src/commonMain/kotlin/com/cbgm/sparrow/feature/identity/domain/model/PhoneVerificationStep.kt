package com.cbgm.sparrow.feature.identity.domain.model

/** Progress of an SMS verification. */
sealed interface PhoneVerificationStep {
    /** The SMS is on its way; a typed code is confirmed with [verificationId]. */
    data class CodeSent(
        val verificationId: String
    ) : PhoneVerificationStep

    /** The platform read the SMS itself, or verified the number without one ([code] is null). */
    data class CodeReceived(
        val code: String?
    ) : PhoneVerificationStep

    /** The number is verified; [token] proves it once to the Control Plane. */
    class Verified(
        val token: String
    ) : PhoneVerificationStep {
        override fun toString(): String = "Verified"
    }
}
