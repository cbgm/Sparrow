package com.cbgm.sparrow.feature.identity.domain.usecase

import com.cbgm.sparrow.feature.identity.domain.model.PhoneVerificationSlotStatus
import com.cbgm.sparrow.feature.identity.domain.repository.PhoneVerificationSlotRepository

/** Asks the phone verifier for today's sign-up slot before any SMS is sent. */
class ReservePhoneVerificationSlotUseCase(
    private val repository: PhoneVerificationSlotRepository
) {
    suspend operator fun invoke(): Result<PhoneVerificationSlotStatus> = repository.reserve()
}
