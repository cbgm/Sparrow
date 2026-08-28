package com.cbgm.sparrow.feature.contacts.domain.usecase

import com.cbgm.sparrow.core.security.ContactBlocklistRepository
import com.cbgm.sparrow.core.security.DirectIdentitySetupModeRepository
import com.cbgm.sparrow.feature.contacts.domain.model.DirectChatAuthorizationRequiredException
import com.cbgm.sparrow.feature.contacts.domain.repository.IdentityInvitationRepository

class RequireDirectChatAuthorizationUseCase(
    private val identityInvitationRepository: IdentityInvitationRepository,
    private val modeRepository: DirectIdentitySetupModeRepository,
    private val contactBlocklistRepository: ContactBlocklistRepository
) {
    suspend operator fun invoke(contactId: String): Result<Unit> =
        runCatching {
            require(contactId.isNotBlank()) {
                "Contact ID must not be blank"
            }

            if (contactBlocklistRepository.isBlocked(contactId)) {
                throw DirectChatAuthorizationRequiredException(
                    "Blocked contacts cannot send or receive direct messages"
                )
            }

            identityInvitationRepository
                .requireDirectChatAuthorization(
                    contactId = contactId,
                    mode = modeRepository.getMode()
                ).getOrThrow()
        }
}
