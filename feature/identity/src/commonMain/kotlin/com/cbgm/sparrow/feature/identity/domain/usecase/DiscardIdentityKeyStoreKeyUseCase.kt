package com.cbgm.sparrow.feature.identity.domain.usecase

import com.cbgm.sparrow.feature.identity.domain.repository.IdentityRepository

/**
 * Developer tool: simulates a lost key-store key. After the next start the identity counts as unusable and
 * the restore-or-reset screen appears.
 */
class DiscardIdentityKeyStoreKeyUseCase(
    private val identityRepository: IdentityRepository
) {
    suspend operator fun invoke(): Result<Unit> = identityRepository.discardKeyStoreKey()
}
