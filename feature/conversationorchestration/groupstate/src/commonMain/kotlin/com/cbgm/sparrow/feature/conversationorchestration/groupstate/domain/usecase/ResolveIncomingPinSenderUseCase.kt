package com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.usecase

import com.cbgm.sparrow.core.util.result.safeSuspendCall
import com.cbgm.sparrow.feature.identity.domain.usecase.FindRemoteIdentityPeerIdUseCase
import com.cbgm.sparrow.feature.membership.domain.model.GroupMetadataMessageSender
import com.cbgm.sparrow.feature.membership.domain.usecase.AuthorizeGroupMetadataUseCase

/**
 * Who wrote the message a received pin names, by its signing key. Membership knows the members of the pin's
 * epoch; an earlier sender falls back to the contact whose pinned identity has that key. The contact ID is
 * null for the local user, or when no contact has the key.
 */
class ResolveIncomingPinSenderUseCase(
    private val authorizeGroupMetadata: AuthorizeGroupMetadataUseCase,
    private val findRemoteIdentityPeerId: FindRemoteIdentityPeerIdUseCase
) {
    suspend operator fun invoke(
        groupId: String,
        epoch: Int,
        senderSigningPublicKey: ByteArray
    ): Result<GroupMetadataMessageSender> =
        safeSuspendCall(logFailure = false) {
            val sender = authorizeGroupMetadata.resolveMessageSender(groupId, epoch, senderSigningPublicKey).getOrThrow()
            if (sender.isLocal) {
                GroupMetadataMessageSender(isLocal = true, memberContactId = null)
            } else {
                GroupMetadataMessageSender(
                    isLocal = false,
                    memberContactId =
                        sender.memberContactId ?: findRemoteIdentityPeerId(senderSigningPublicKey).getOrThrow()
                )
            }
        }
}
