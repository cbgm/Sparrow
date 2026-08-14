package com.cbgm.securechat.feature.identity.domain.usecase

import com.cbgm.securechat.feature.identity.domain.repository.LocalIdentityProfileRepository

class SaveLocalPhoneNameUseCase(
    private val localIdentityProfileRepository: LocalIdentityProfileRepository
) {
    suspend operator fun invoke(
        phoneNumber: String,
        name: String
    ): Result<Unit> =
        localIdentityProfileRepository.savePhoneName(
            phoneNumber = phoneNumber,
            name = name
        )
}
