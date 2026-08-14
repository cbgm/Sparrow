package com.cbgm.sparrow.feature.identity.domain.usecase

import com.cbgm.sparrow.feature.identity.domain.model.PublicIdentity
import com.cbgm.sparrow.feature.identity.domain.repository.IdentityRepository

class CreateIdentityUseCase(
    private val repository: IdentityRepository
) {
    suspend operator fun invoke(): Result<PublicIdentity> = repository.createIdentity()
}
