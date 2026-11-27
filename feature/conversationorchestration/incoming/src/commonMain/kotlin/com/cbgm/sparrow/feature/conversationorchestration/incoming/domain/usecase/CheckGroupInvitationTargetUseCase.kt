package com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.usecase

import com.cbgm.sparrow.core.util.result.safeSuspendCall
import com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.error.rejectUnless
import com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.policy.GroupInvitationTargetPolicy
import com.cbgm.sparrow.feature.conversationorchestration.membership.domain.repository.GroupConversationRepository
import com.cbgm.sparrow.feature.membership.domain.model.IncomingMembershipOffer
import com.cbgm.sparrow.feature.membership.domain.usecase.InspectGroupInvitationTargetUseCase

/**
 * Rejects an invitation whose group ID names a conversation it must not overwrite
 * ([GroupInvitationTargetPolicy]); the rejected envelope is dropped.
 */
class CheckGroupInvitationTargetUseCase(
    private val conversationRepository: GroupConversationRepository,
    private val inspectGroupInvitationTarget: InspectGroupInvitationTargetUseCase
) {
    suspend operator fun invoke(offer: IncomingMembershipOffer, inviterContactId: String): Result<Unit> =
        safeSuspendCall(logFailure = false) {
            val conversation = conversationRepository.findKind(offer.groupId).getOrThrow()
            val target = inspectGroupInvitationTarget(offer.groupId).getOrThrow()
            rejectUnless(GroupInvitationTargetPolicy.mayStage(conversation, target, inviterContactId)) {
                "Group invitation names an existing conversation that is not its pending invitation"
            }
        }
}
