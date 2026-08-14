package com.cbgm.sparrow.feature.identity.domain.usecase

import com.cbgm.sparrow.feature.identity.domain.model.IdentityStatus
import com.cbgm.sparrow.feature.identity.domain.repository.IdentityRepository

/**
 * Retrieves the current state of the user's local identity.
 *
 * Presentation code uses this use case instead of accessing
 * storage or the repository implementation directly.
 */
class GetIdentityStatusUseCase(
    private val repository: IdentityRepository
) {
    suspend operator fun invoke(): Result<IdentityStatus> = repository.getStatus()
}
