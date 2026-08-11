package com.cbgm.securechat.feature.contacts.domain.usecase

import com.cbgm.securechat.feature.contacts.domain.identity.IdentityInvitationService
import com.cbgm.securechat.feature.contacts.domain.model.IdentityHandshakeState
import kotlinx.coroutines.flow.Flow

class ObserveIdentityHandshakeState(
    private val identityInvitationService: IdentityInvitationService
) {
    operator fun invoke(contactId: String): Flow<IdentityHandshakeState?> =
        identityInvitationService.observeState(contactId)
}
