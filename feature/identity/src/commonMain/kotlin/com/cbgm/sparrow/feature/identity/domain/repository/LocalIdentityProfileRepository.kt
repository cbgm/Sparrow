package com.cbgm.sparrow.feature.identity.domain.repository

import kotlinx.coroutines.flow.Flow

interface LocalIdentityProfileRepository {
    fun observePhoneNumber(): Flow<String?>

    suspend fun loadPhoneName(): Result<Pair<String, String>?>

    suspend fun savePhoneName(
        phoneNumber: String,
        name: String
    ): Result<Unit>

    /** Keeps the token in memory for the first route registration; it is never stored. */
    fun keepPhoneVerificationToken(token: String)

    /** True while the phone verifier's daily maximum keeps the number unverified. */
    fun observePhoneVerificationLimitReached(): Flow<Boolean>

    suspend fun loadPhoneNumber(): Result<String?>

    suspend fun deletePhoneName(): Result<Unit>
}
