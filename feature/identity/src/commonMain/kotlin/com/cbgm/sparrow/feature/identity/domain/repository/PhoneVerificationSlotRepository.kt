package com.cbgm.sparrow.feature.identity.domain.repository

import com.cbgm.sparrow.feature.identity.domain.model.PhoneVerificationSlotStatus

interface PhoneVerificationSlotRepository {
    /** Reserves a sign-up slot at the phone verifier and keeps it in memory for the attestation. */
    suspend fun reserve(): Result<PhoneVerificationSlotStatus>
}
