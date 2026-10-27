package com.cbgm.sparrow.feature.conversationorchestration.membership.domain.usecase

import com.cbgm.sparrow.core.util.result.safeSuspendCall
import com.cbgm.sparrow.feature.identity.domain.model.KeyExchangeStatus
import com.cbgm.sparrow.feature.identity.domain.model.RemotePeerIdentity
import com.cbgm.sparrow.feature.identity.domain.usecase.GetRemoteIdentityUseCase

/**
 * The pinned identity of the group admin a received group packet must be signed by. The packets differ in how
 * established that identity must be, so each has its own function.
 */
class GetTrustedGroupAuthorityUseCase(
    private val getRemoteIdentity: GetRemoteIdentityUseCase
) {
    /** The owner who activates members; only a mutual key exchange is trusted. */
    suspend fun forActivation(ownerContactId: String): Result<RemotePeerIdentity> =
        safeSuspendCall(logFailure = false) {
            val owner = load(ownerContactId) { "Group owner has no Sparrow identity" }
            check(owner.keyExchangeStatus == KeyExchangeStatus.MUTUAL) { "Group owner key exchange is not mutual" }
            owner
        }

    /** The owner who deleted the group; the key exchange need not be mutual. */
    suspend fun forDeletion(ownerContactId: String): Result<RemotePeerIdentity> =
        safeSuspendCall(logFailure = false) { load(ownerContactId) { "Group owner identity was not found" } }

    /**
     * The inviting admin of a first welcome, or null for a later welcome: Membership checks those against the
     * admin of the prior epoch instead. The key exchange need not be mutual yet; the first welcome establishes it.
     */
    suspend fun forWelcome(adminContactId: String, isFirstWelcome: Boolean): Result<RemotePeerIdentity?> =
        safeSuspendCall(logFailure = false) {
            if (isFirstWelcome) {
                load(adminContactId) { "Inviting group admin has no accepted Sparrow identity" }
            } else {
                null
            }
        }

    private suspend fun load(contactId: String, missingMessage: () -> String): RemotePeerIdentity =
        getRemoteIdentity(contactId).getOrThrow() ?: error(missingMessage())
}
