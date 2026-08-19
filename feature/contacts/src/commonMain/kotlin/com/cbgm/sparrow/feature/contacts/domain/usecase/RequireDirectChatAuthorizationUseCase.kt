package com.cbgm.sparrow.feature.contacts.domain.usecase

import com.cbgm.sparrow.feature.contacts.domain.repository.IdentityInvitationRepository

class RequireDirectChatAuthorizationUseCase(
    private val repository: IdentityInvitationRepository
) {
    suspend operator fun invoke(contactId: String): Result<Unit> =
        repository.requireDirectChatAuthorization(contactId)
}
