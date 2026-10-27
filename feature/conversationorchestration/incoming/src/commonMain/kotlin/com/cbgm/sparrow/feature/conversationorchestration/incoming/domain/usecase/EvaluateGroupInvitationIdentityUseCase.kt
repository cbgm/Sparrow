package com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.usecase

import com.cbgm.sparrow.core.util.result.safeSuspendCall
import com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.policy.GroupInvitationIdentityDisposition
import com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.policy.GroupInvitationIdentityPolicy
import com.cbgm.sparrow.feature.identity.domain.model.hasKeys
import com.cbgm.sparrow.feature.identity.domain.usecase.GetRemoteIdentityUseCase

/** Compares the keys a group offer or JOIN proposes for [peerId] with the identity stored for that peer. */
class EvaluateGroupInvitationIdentityUseCase(
    private val getRemoteIdentity: GetRemoteIdentityUseCase
) {
    suspend operator fun invoke(
        peerId: String,
        offeredEncryptionPublicKey: ByteArray,
        offeredSigningPublicKey: ByteArray
    ): Result<GroupInvitationIdentityDisposition> =
        safeSuspendCall(logFailure = false) {
            val stored = getRemoteIdentity(peerId).getOrThrow()
            GroupInvitationIdentityPolicy.evaluate(
                storedEncryptionPublicKey = stored?.encryptionPublicKey,
                storedSigningPublicKey = stored?.signingPublicKey,
                offeredEncryptionPublicKey = offeredEncryptionPublicKey,
                offeredSigningPublicKey = offeredSigningPublicKey
            )
        }

    /**
     * Whether [peerId] is pinned to exactly the offered keys. Unlike [invoke] the keys are not validated: an
     * unknown peer or incomplete keys simply do not match.
     */
    suspend fun isPinned(
        peerId: String,
        offeredEncryptionPublicKey: ByteArray,
        offeredSigningPublicKey: ByteArray
    ): Result<Boolean> =
        safeSuspendCall(logFailure = false) {
            getRemoteIdentity(peerId).getOrThrow()?.hasKeys(offeredEncryptionPublicKey, offeredSigningPublicKey) == true
        }
}
