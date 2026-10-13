package com.cbgm.sparrow.feature.conversationorchestration.membership.domain.usecase

import com.cbgm.sparrow.feature.conversationorchestration.membership.domain.model.GroupVerificationSigner
import com.cbgm.sparrow.protocol.identity.LocalPublicIdentityProvider
import com.cbgm.sparrow.protocol.identity.LocalSigningKeyPairProvider

class LoadGroupVerificationSignerUseCase(
    private val localPublicIdentityProvider: LocalPublicIdentityProvider,
    private val localSigningKeyPairProvider: LocalSigningKeyPairProvider
) {
    suspend operator fun invoke(): Result<GroupVerificationSigner> =
        runCatching {
            GroupVerificationSigner(
                identity = localPublicIdentityProvider.getLocalPublicIdentity().getOrThrow(),
                signingKeyPair = localSigningKeyPairProvider.getSigningKeyPair().getOrThrow()
            )
        }
}
