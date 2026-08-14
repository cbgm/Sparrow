package com.cbgm.sparrow.feature.identity.domain.usecase

import com.cbgm.sparrow.feature.identity.domain.repository.LocalIdentityProfileRepository

class GetLocalPhoneNumberUseCase(
    private val localIdentityProfileRepository: LocalIdentityProfileRepository
) {
    suspend operator fun invoke(): Result<String?> = localIdentityProfileRepository.loadPhoneNumber()
}
