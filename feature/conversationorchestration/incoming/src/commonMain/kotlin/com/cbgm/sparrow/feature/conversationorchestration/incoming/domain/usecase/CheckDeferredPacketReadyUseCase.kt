package com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.usecase

import com.cbgm.sparrow.core.util.result.safeSuspendCall
import com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.model.DeferredPacketWait
import com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.model.DirectPeerAccess
import com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.policy.GroupPacketEpochPolicy
import com.cbgm.sparrow.feature.membership.domain.usecase.FindGroupCurrentEpochUseCase
import com.cbgm.sparrow.feature.membership.domain.usecase.WasGroupMembershipDeletedUseCase

/**
 * Whether parked envelopes waiting for [DeferredPacketWait] should be processed again. That is also the case
 * when the wait can no longer be met (the group was deleted, the invitation failed or the contact was blocked):
 * processing them again drops them now instead of keeping them until they expire.
 */
class CheckDeferredPacketReadyUseCase(
    private val findGroupCurrentEpoch: FindGroupCurrentEpochUseCase,
    private val wasGroupMembershipDeleted: WasGroupMembershipDeletedUseCase,
    private val resolveDirectPeerAccess: ResolveDirectPeerAccessUseCase
) {
    suspend operator fun invoke(wait: DeferredPacketWait): Result<Boolean> =
        safeSuspendCall(logFailure = false) {
            when (wait) {
                is DeferredPacketWait.GroupEpoch ->
                    GroupPacketEpochPolicy.isInstalled(findGroupCurrentEpoch(wait.groupId).getOrThrow(), wait.epoch) ||
                        wasGroupMembershipDeleted(wait.groupId).getOrThrow()
                is DeferredPacketWait.ContactExchange ->
                    resolveDirectPeerAccess(wait.contactId).getOrThrow() != DirectPeerAccess.PENDING
            }
        }
}
