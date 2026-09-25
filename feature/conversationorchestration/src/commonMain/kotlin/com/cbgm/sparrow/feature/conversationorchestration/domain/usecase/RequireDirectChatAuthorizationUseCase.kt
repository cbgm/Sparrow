package com.cbgm.sparrow.feature.conversationorchestration.domain.usecase

import com.cbgm.sparrow.feature.contacts.domain.repository.ContactBlocklistRepository
import com.cbgm.sparrow.feature.conversationorchestration.domain.error.DirectChatAuthorizationRequiredException
import com.cbgm.sparrow.feature.identity.domain.model.hasDirectMessageEncryptionKeys
import com.cbgm.sparrow.feature.identity.domain.usecase.GetIdentityPeerStateUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.GetRemoteIdentityUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.ObservePendingRemoteIdentityChangesUseCase
import kotlinx.coroutines.flow.first

class RequireDirectChatAuthorizationUseCase(
    private val getIdentityPeerState: GetIdentityPeerStateUseCase,
    private val contactBlocklistRepository: ContactBlocklistRepository,
    private val getRemoteIdentity: GetRemoteIdentityUseCase,
    private val observePendingRemoteIdentityChanges: ObservePendingRemoteIdentityChangesUseCase
) {
    suspend operator fun invoke(contactId: String): Result<Unit> =
        runCatching {
            require(contactId.isNotBlank()) { "Contact ID must not be blank" }
            if (contactBlocklistRepository.isBlocked(contactId)) {
                throw DirectChatAuthorizationRequiredException(
                    "Blocked contacts cannot send or receive direct messages"
                )
            }
            // A prior MUTUAL exchange can remain in the database while a new
            // identity-change request awaits explicit verification. Never let
            // any caller bypass the pending-replacement gate just because it
            // skipped PrepareConversationMessageUseCase.
            if (observePendingRemoteIdentityChanges().first().any { it.peerId == contactId }) {
                throw DirectChatAuthorizationRequiredException(
                    "Identity change pending verification; messages must wait for recovery"
                )
            }
            val state = getIdentityPeerState(contactId).getOrThrow()
            if (!state.hasEstablishedExchange ||
                !getRemoteIdentity(contactId).getOrThrow().hasDirectMessageEncryptionKeys()
            ) {
                throw DirectChatAuthorizationRequiredException(
                    "Mutual identity authorization and valid encryption keys are required before direct messages can be sent"
                )
            }
        }
}
