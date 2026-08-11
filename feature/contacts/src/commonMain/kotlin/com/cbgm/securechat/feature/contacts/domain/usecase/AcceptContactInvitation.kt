package com.cbgm.securechat.feature.contacts.domain.usecase

import com.cbgm.securechat.feature.contacts.domain.identity.IdentityInvitationService

class AcceptContactInvitation(
    private val identityInvitationService: IdentityInvitationService
) {
    suspend operator fun invoke(invitationId: String): Result<Unit> =
        identityInvitationService.accept(invitationId)
}
