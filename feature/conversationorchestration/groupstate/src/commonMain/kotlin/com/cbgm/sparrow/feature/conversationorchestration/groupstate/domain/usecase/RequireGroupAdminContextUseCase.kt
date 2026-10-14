package com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.usecase

import com.cbgm.sparrow.core.result.safeSuspendCall
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.model.GroupAdminContext
import com.cbgm.sparrow.feature.membership.domain.usecase.AuthorizeGroupMetadataUseCase
import com.cbgm.sparrow.protocol.identity.LocalSigningKeyPairProvider

/** Fails unless the local member is a group admin; the context signs and addresses group metadata. */
class RequireGroupAdminContextUseCase(
    private val authorizeGroupMetadata: AuthorizeGroupMetadataUseCase,
    private val localSigningKeyPairProvider: LocalSigningKeyPairProvider
) {
    suspend operator fun invoke(groupId: String, action: String): Result<GroupAdminContext> =
        safeSuspendCall(logFailure = false) {
            require(groupId.isNotBlank()) { "Group ID must not be blank" }
            val signingKeyPair = localSigningKeyPairProvider.getSigningKeyPair().getOrThrow()
            val authorized = authorizeGroupMetadata.send(
                groupId = groupId,
                localSigningPublicKey = signingKeyPair.publicKey,
                action = action
            ).getOrThrow()
            GroupAdminContext(
                groupId = groupId,
                epoch = authorized.epoch,
                signingKeyPair = signingKeyPair,
                recipientContactIds = authorized.recipientContactIds
            )
        }
}
