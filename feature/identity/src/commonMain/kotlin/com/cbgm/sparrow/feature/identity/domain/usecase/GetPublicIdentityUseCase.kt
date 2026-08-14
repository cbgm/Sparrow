package com.cbgm.sparrow.feature.identity.domain.usecase

import com.cbgm.sparrow.feature.identity.domain.model.PublicIdentity
import com.cbgm.sparrow.feature.identity.domain.repository.IdentityRepository

/**
 * Loads the public part of the user's local identity.
 *
 * Private keys are never returned here.
 */
class GetPublicIdentityUseCase(
    private val repository: IdentityRepository
) {
    suspend operator fun invoke(): Result<PublicIdentity?> = repository.getIdentity()
}
