package com.cbgm.sparrow.feature.autoreply.domain.usecase

import com.cbgm.sparrow.feature.autoreply.domain.repository.AutoReplyRepository

class ReleaseAutoReplyRecipientUseCase(
    private val repository: AutoReplyRepository
) {
    suspend operator fun invoke(
        contactId: String,
        expectedActivationSessionId: String
    ): Result<Unit> = repository.releaseContactClaim(contactId, expectedActivationSessionId)
}
