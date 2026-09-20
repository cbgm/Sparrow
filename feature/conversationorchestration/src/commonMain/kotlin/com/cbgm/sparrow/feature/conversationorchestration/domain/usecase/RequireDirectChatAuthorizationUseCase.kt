package com.cbgm.sparrow.feature.conversationorchestration.domain.usecase

import com.cbgm.sparrow.core.security.ContactBlocklistRepository
import com.cbgm.sparrow.core.security.DirectChatAuthorizationRequiredException
import com.cbgm.sparrow.core.security.DirectIdentitySetupMode
import com.cbgm.sparrow.core.security.DirectIdentitySetupModeRepository
import com.cbgm.sparrow.feature.identity.domain.usecase.GetIdentityPeerStateUseCase

class RequireDirectChatAuthorizationUseCase(
    private val getIdentityPeerState: GetIdentityPeerStateUseCase,
    private val modeRepository: DirectIdentitySetupModeRepository,
    private val contactBlocklistRepository: ContactBlocklistRepository
) {
    suspend operator fun invoke(contactId: String): Result<Unit> =
        runCatching {
            require(contactId.isNotBlank()) { "Contact ID must not be blank" }
            if (contactBlocklistRepository.isBlocked(contactId)) {
                throw DirectChatAuthorizationRequiredException(
                    "Blocked contacts cannot send or receive direct messages"
                )
            }
            val state = getIdentityPeerState(contactId).getOrThrow()
            when (modeRepository.getMode()) {
                DirectIdentitySetupMode.AUTOMATIC_INVITATION ->
                    if (!state.hasEstablishedExchange) {
                        throw DirectChatAuthorizationRequiredException(
                            "A contact invitation must be accepted before messages can be sent"
                        )
                    }
                DirectIdentitySetupMode.MANUAL_IDENTITY_SHARING ->
                    if (!state.hasEstablishedExchange || !state.hasMutualIdentity) {
                        throw DirectChatAuthorizationRequiredException(
                            "A contact invitation must be accepted and both identities exchanged before messages can be sent"
                        )
                    }
            }
        }
}
