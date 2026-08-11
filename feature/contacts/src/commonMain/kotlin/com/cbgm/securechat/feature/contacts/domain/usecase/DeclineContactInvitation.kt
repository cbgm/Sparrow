package com.cbgm.securechat.feature.contacts.domain.usecase

import com.cbgm.securechat.feature.contacts.domain.identity.IdentityInvitationService

class DeclineContactInvitation(
    private val identityInvitationService: IdentityInvitationService
) {
    suspend operator fun invoke(invitationId: String): Result<Unit> =
        identityInvitationService.decline(invitationId)
}
