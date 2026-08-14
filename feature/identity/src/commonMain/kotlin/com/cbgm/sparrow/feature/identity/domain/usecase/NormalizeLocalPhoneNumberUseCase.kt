package com.cbgm.sparrow.feature.identity.domain.usecase

import com.cbgm.sparrow.core.protocol.phone.PhoneNumberNormalizer

class NormalizeLocalPhoneNumberUseCase(
    private val phoneNumberNormalizer: PhoneNumberNormalizer
) {
    operator fun invoke(phoneNumber: String): Result<String> = phoneNumberNormalizer.normalize(phoneNumber = phoneNumber)
}
