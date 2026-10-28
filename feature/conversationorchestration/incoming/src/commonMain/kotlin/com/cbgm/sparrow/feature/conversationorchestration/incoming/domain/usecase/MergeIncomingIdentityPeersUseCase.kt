package com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.usecase

import com.cbgm.sparrow.core.util.result.safeSuspendCall
import com.cbgm.sparrow.feature.contacts.domain.model.identity.IdentityPeerMerge
import com.cbgm.sparrow.feature.contacts.domain.usecase.identity.ApplyIdentityPeerMergeUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.ReassignIdentityExchangePeerUseCase

/** Merges the duplicate peers of an incoming identity: Identity reassigns each exchange, then Contacts merges the peer. */
class MergeIncomingIdentityPeersUseCase(
    private val reassignIdentityExchangePeer: ReassignIdentityExchangePeerUseCase,
    private val applyIdentityPeerMerge: ApplyIdentityPeerMergeUseCase
) {
    suspend operator fun invoke(merges: List<IdentityPeerMerge>): Result<Unit> =
        safeSuspendCall(logFailure = false) {
            merges.forEach { merge ->
                reassignIdentityExchangePeer(merge.fromPeerId, merge.toPeerId).getOrThrow()
                applyIdentityPeerMerge(merge).getOrThrow()
            }
        }
}
