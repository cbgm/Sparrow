package com.cbgm.sparrow.core.phone

/** Supplies a short-lived, Firebase-issued proof for the current local number. */
interface PhoneVerificationProofProvider {
    suspend fun tokenFor(phoneNumber: String): String?
}
