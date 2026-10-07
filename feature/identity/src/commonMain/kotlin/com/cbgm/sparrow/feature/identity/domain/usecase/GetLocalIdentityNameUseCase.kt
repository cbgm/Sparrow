package com.cbgm.sparrow.feature.identity.domain.usecase

import com.cbgm.sparrow.feature.identity.domain.repository.LocalIdentityProfileRepository

class GetLocalIdentityNameUseCase(
    private val localIdentityProfileRepository: LocalIdentityProfileRepository
) {
    suspend operator fun invoke(): Result<String?> =
        localIdentityProfileRepository
            .loadPhoneName()
            .map { phoneName -> phoneName?.second?.trim()?.takeIf(String::isNotEmpty) }
}
