package com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.usecase

import com.cbgm.sparrow.core.util.result.safeSuspendCall
import com.cbgm.sparrow.feature.contacts.domain.model.identity.IdentityPeerResolution
import com.cbgm.sparrow.feature.contacts.domain.usecase.BlockContactUseCase
import com.cbgm.sparrow.feature.contacts.domain.usecase.identity.ApplyIdentityPeerMergeUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.ReassignIdentityExchangePeerUseCase

/**
 * Merges the duplicate peers of an incoming identity: Identity reassigns each exchange, then Contacts merges the
 * peer. A block is never lost: the resolved peer is blocked first when it inherits one (see
 * [IdentityPeerResolution.inheritsBlock]), so a merge cannot delete the only blocked contact.
 */
class MergeIncomingIdentityPeersUseCase(
    private val reassignIdentityExchangePeer: ReassignIdentityExchangePeerUseCase,
    private val applyIdentityPeerMerge: ApplyIdentityPeerMergeUseCase,
    private val blockContact: BlockContactUseCase
) {
    suspend operator fun invoke(resolution: IdentityPeerResolution): Result<Unit> =
        safeSuspendCall(logFailure = false) {
            if (resolution.inheritsBlock) blockContact(resolution.peerId).getOrThrow()
            resolution.merges.forEach { merge ->
                reassignIdentityExchangePeer(merge.fromPeerId, merge.toPeerId).getOrThrow()
                applyIdentityPeerMerge(merge).getOrThrow()
            }
        }
}
