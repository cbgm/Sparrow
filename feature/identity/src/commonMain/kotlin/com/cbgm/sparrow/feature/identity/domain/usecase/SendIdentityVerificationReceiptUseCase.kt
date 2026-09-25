package com.cbgm.sparrow.feature.identity.domain.usecase

import com.cbgm.sparrow.feature.identity.domain.repository.IdentityVerificationRepository

class SendIdentityVerificationReceiptUseCase(
    private val repository: IdentityVerificationRepository
) {
    suspend operator fun invoke(contactId: String): Result<Unit> =
        repository.sendReceiptIfLocallyVerified(contactId)
}
