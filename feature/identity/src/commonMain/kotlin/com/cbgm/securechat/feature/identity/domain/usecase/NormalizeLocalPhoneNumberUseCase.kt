package com.cbgm.securechat.feature.identity.domain.usecase

import com.cbgm.securechat.core.protocol.phone.PhoneNumberNormalizer

class NormalizeLocalPhoneNumberUseCase(
    private val phoneNumberNormalizer: PhoneNumberNormalizer
) {
    operator fun invoke(phoneNumber: String): Result<String> = phoneNumberNormalizer.normalize(phoneNumber = phoneNumber)
}
