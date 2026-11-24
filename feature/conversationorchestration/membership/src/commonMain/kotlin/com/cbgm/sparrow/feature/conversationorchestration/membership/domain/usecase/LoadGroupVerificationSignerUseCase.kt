package com.cbgm.sparrow.feature.conversationorchestration.membership.domain.usecase

import com.cbgm.sparrow.core.util.result.safeSuspendCall
import com.cbgm.sparrow.feature.conversationorchestration.membership.domain.model.GroupVerificationSigner
import com.cbgm.sparrow.protocol.identity.LocalPublicIdentitySource
import com.cbgm.sparrow.protocol.identity.LocalSigningKeyPairSource

class LoadGroupVerificationSignerUseCase(
    private val localPublicIdentitySource: LocalPublicIdentitySource,
    private val localSigningKeyPairSource: LocalSigningKeyPairSource
) {
    suspend operator fun invoke(): Result<GroupVerificationSigner> =
        safeSuspendCall(logFailure = false) {
            GroupVerificationSigner(
                identity = localPublicIdentitySource.getLocalPublicIdentity().getOrThrow(),
                signingKeyPair = localSigningKeyPairSource.getSigningKeyPair().getOrThrow()
            )
        }
}
