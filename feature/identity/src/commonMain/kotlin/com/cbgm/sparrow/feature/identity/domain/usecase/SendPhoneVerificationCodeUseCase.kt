package com.cbgm.sparrow.feature.identity.domain.usecase

import com.cbgm.sparrow.feature.identity.domain.model.PhoneVerificationStep
import com.cbgm.sparrow.feature.identity.domain.repository.LocalIdentityProfileRepository
import com.cbgm.sparrow.feature.identity.domain.repository.PhoneVerificationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onEach

/** Sends the SMS; the token of a verified number stays in memory for the first route. */
class SendPhoneVerificationCodeUseCase(
    private val repository: PhoneVerificationRepository,
    private val localIdentityProfileRepository: LocalIdentityProfileRepository
) {
    operator fun invoke(phoneNumber: String): Flow<Result<PhoneVerificationStep>> =
        repository.sendCode(phoneNumber).onEach { result ->
            val step = result.getOrNull()
            if (step is PhoneVerificationStep.Verified) {
                localIdentityProfileRepository.keepPhoneVerificationToken(step.token)
            }
        }
}
