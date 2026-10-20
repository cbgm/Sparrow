package com.cbgm.sparrow.feature.identity.domain.usecase

import com.cbgm.sparrow.feature.identity.domain.repository.PhoneVerificationRepository

class SendPhoneVerificationCodeUseCase(
    private val repository: PhoneVerificationRepository
) {
    suspend operator fun invoke(
        phoneNumber: String,
        onVerifiedAutomatically: (String?) -> Unit
    ): Result<String> = repository.sendCode(phoneNumber, onVerifiedAutomatically)
}
