package com.cbgm.sparrow.feature.identity.domain.repository

interface PhoneVerificationRepository {
    /**
     * Sends the SMS code and returns the verification ID. When the platform verifies the number
     * by itself (it read the SMS, or the test mode supplied the code), [onVerifiedAutomatically]
     * is called with the code if one is known; [confirmCode] then needs no typed code.
     */
    suspend fun sendCode(
        phoneNumber: String,
        onVerifiedAutomatically: (String?) -> Unit
    ): Result<String>

    /** Returns the one-time token proving the number to the Control Plane. */
    suspend fun confirmCode(
        verificationId: String,
        code: String
    ): Result<String>
}
