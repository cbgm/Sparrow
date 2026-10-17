package com.cbgm.sparrow.feature.identity.domain.usecase

import com.cbgm.sparrow.core.phone.PhoneVerificationPolicyProvider

class GetPhoneVerificationPolicyUseCase(
    private val provider: PhoneVerificationPolicyProvider
) {
    suspend operator fun invoke(): Result<Boolean> = provider.isRequired()
}
