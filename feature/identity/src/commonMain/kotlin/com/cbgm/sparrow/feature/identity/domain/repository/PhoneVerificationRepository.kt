package com.cbgm.sparrow.feature.identity.domain.repository

import com.cbgm.sparrow.feature.identity.domain.model.PhoneVerificationStep
import kotlinx.coroutines.flow.Flow

interface PhoneVerificationRepository {
    /** Sends the SMS code and reports the progress until the collector stops. */
    fun sendCode(phoneNumber: String): Flow<Result<PhoneVerificationStep>>

    /** Returns the one-time token proving the number to the Control Plane. */
    suspend fun confirmCode(
        verificationId: String,
        code: String
    ): Result<String>
}
