package com.cbgm.sparrow.feature.contacts.domain.usecase

import com.cbgm.sparrow.feature.contacts.domain.repository.IdentityExchangeRepository

class EnsureIdentityExchangeStartedUseCase(
    private val identityExchangeRepository: IdentityExchangeRepository
) {
    suspend operator fun invoke(contactId: String): Result<Unit> =
        identityExchangeRepository.ensureStarted(contactId)
}
