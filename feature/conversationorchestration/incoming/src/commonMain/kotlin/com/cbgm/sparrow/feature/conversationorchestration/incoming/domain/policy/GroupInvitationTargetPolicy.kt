package com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.policy

import com.cbgm.sparrow.feature.conversationorchestration.membership.domain.model.GroupConversationKind
import com.cbgm.sparrow.feature.membership.domain.model.GroupInvitationTarget

/**
 * An invitation stages its group conversation before it is accepted, and the signature proves only the
 * inviter's own keys, not that the group ID belongs to them. So an invitation may stage only a group ID that is
 * new to the device or names a group the local user is not a live member of, and only when no other inviter has
 * a pending invitation into it. Otherwise a member of a group could rewrite that group's title and creation time,
 * or turn a direct chat into a group by sending an invitation with its ID.
 *
 * Inviting again after leaving or removal keeps working: the old group is no longer live.
 */
object GroupInvitationTargetPolicy {
    fun mayStage(
        conversation: GroupConversationKind,
        target: GroupInvitationTarget,
        inviterContactId: String
    ): Boolean =
        conversation != GroupConversationKind.OTHER &&
            !target.hasLiveMembership &&
            target.pendingInviterContactIds.all { pendingInviter -> pendingInviter == inviterContactId }
}
