package com.cbgm.sparrow.feature.conversationorchestration.lifecycle.domain.usecase

import com.cbgm.sparrow.core.util.result.safeSuspendCall
import com.cbgm.sparrow.feature.identity.domain.model.IdentityExchangeOffer
import com.cbgm.sparrow.feature.identity.domain.usecase.GetRemoteIdentityUseCase

/** The approved invitation is accepted only while its keys are still the contact's pinned identity. */
class RequireApprovedIdentityPinnedUseCase(
    private val getRemoteIdentity: GetRemoteIdentityUseCase
) {
    suspend operator fun invoke(peerId: String, offer: IdentityExchangeOffer): Result<Unit> =
        safeSuspendCall(logFailure = false) {
            val pinned =
                requireNotNull(getRemoteIdentity(peerId).getOrThrow()) {
                    "Approved replacement identity is missing"
                }
            check(
                pinned.encryptionPublicKey.contentEquals(offer.encryptionPublicKey) &&
                    pinned.signingPublicKey.contentEquals(offer.signingPublicKey)
            ) { "Approved invitation no longer matches the pinned contact identity" }
        }
}
