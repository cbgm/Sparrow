package com.cbgm.sparrow.core.util.phone

interface LocalPhoneNumberProvider {
    suspend fun getLocalPhoneNumber(): Result<String>

    /**
     * One-time proof of the number verified at sign-up. The first route registration sends it;
     * afterwards it is cleared and never sent again.
     */
    suspend fun getPhoneVerificationToken(): Result<String?>

    suspend fun clearPhoneVerificationToken(): Result<Unit>
}
