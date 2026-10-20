package com.cbgm.sparrow.feature.identity.domain.usecase

import com.cbgm.sparrow.feature.identity.domain.repository.LocalIdentityProfileRepository
import com.cbgm.sparrow.feature.identity.domain.repository.PhoneVerificationRepository

/** Confirms the SMS code; the token proving the number stays in memory for the first route. */
class ConfirmPhoneVerificationCodeUseCase(
    private val repository: PhoneVerificationRepository,
    private val localIdentityProfileRepository: LocalIdentityProfileRepository
) {
    suspend operator fun invoke(
        verificationId: String,
        code: String
    ): Result<Unit> =
        repository.confirmCode(verificationId, code).map { token ->
            localIdentityProfileRepository.keepPhoneVerificationToken(token)
        }
}
