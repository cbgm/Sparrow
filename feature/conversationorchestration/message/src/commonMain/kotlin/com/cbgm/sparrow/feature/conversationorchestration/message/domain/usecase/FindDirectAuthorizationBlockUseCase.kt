package com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase

import com.cbgm.sparrow.core.util.result.safeSuspendCall
import com.cbgm.sparrow.feature.identity.domain.model.hasDirectMessageEncryptionKeys
import com.cbgm.sparrow.feature.identity.domain.usecase.GetIdentityPeerStateUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.GetRemoteIdentityUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.ObservePendingRemoteIdentityChangesUseCase
import kotlinx.coroutines.flow.first

/** Why direct messages to a contact cannot be encrypted right now, or null when they can. */
class FindDirectAuthorizationBlockUseCase(
    private val getIdentityPeerState: GetIdentityPeerStateUseCase,
    private val getRemoteIdentity: GetRemoteIdentityUseCase,
    private val observePendingRemoteIdentityChanges: ObservePendingRemoteIdentityChangesUseCase
) {
    suspend operator fun invoke(contactId: String): Result<String?> =
        safeSuspendCall(logFailure = false) {
            // A prior MUTUAL exchange can remain in the database while a new
            // identity-change request awaits explicit verification.
            if (isIdentityChangePending(contactId)) return@safeSuspendCall PENDING_IDENTITY_CHANGE
            val state = getIdentityPeerState(contactId).getOrThrow()
            if (!state.hasEstablishedExchange ||
                !getRemoteIdentity(contactId).getOrThrow().hasDirectMessageEncryptionKeys()
            ) {
                return@safeSuspendCall MISSING_AUTHORIZATION
            }
            null
        }

    /** A new identity of the contact awaits verification; nothing may be encrypted for the old keys. */
    suspend fun hasPendingIdentityChange(contactId: String): Result<Boolean> =
        safeSuspendCall(logFailure = false) { isIdentityChangePending(contactId) }

    private suspend fun isIdentityChangePending(contactId: String): Boolean =
        observePendingRemoteIdentityChanges().first().any { it.peerId == contactId }

    private companion object {
        const val PENDING_IDENTITY_CHANGE = "Identity change pending verification; messages must wait for recovery"
        const val MISSING_AUTHORIZATION =
            "Mutual identity authorization and valid encryption keys are required before direct messages can be sent"
    }
}
