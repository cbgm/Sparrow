package com.cbgm.sparrow.protocol.phone

/**
 * Sign-up slots of the phone verifier next to the Control Plane Directory. A slot is reserved before the SMS is sent,
 * so a full day costs no SMS; the phone attestation uses it up.
 */
interface PhoneVerificationSlotSource {
    /** Throws when the verifier cannot be reached. */
    suspend fun reserve(): PhoneVerificationSlot
}

sealed interface PhoneVerificationSlot {
    /** [slot] goes with the attestation; the SMS code may be sent [resends] more times. */
    data class Reserved(
        val slot: String,
        val expiresAtEpochMilliseconds: Long,
        val resends: Int
    ) : PhoneVerificationSlot {
        override fun toString(): String = "Reserved(slot=redacted, resends=$resends)"
    }

    /** Today's sign-ups are used up; the next day starts at [retryAtEpochMilliseconds]. */
    data class DayFull(
        val retryAtEpochMilliseconds: Long
    ) : PhoneVerificationSlot

    /** The Directory verifies no phone numbers: the SMS is sent without a slot, and the number stays unverified. */
    data object NotOffered : PhoneVerificationSlot
}
