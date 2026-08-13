package com.cbgm.securechat.feature.contacts.domain.usecase

import com.cbgm.securechat.feature.contacts.domain.repository.IdentityInvitationRepository

class DeclineContactInvitationUseCase(
    private val identityInvitationRepository: IdentityInvitationRepository
) {
    suspend operator fun invoke(invitationId: String): Result<Unit> =
        identityInvitationRepository.decline(invitationId)
}
