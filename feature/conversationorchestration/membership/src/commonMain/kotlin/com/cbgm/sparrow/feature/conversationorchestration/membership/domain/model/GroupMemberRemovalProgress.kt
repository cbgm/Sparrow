package com.cbgm.sparrow.feature.conversationorchestration.membership.domain.model

import com.cbgm.sparrow.feature.membership.domain.model.GroupCommittedMemberRemoval
import com.cbgm.sparrow.feature.membership.domain.model.GroupMemberRemovalResult

/**
 * How far an admin's removal of a member got. Membership commits the rotation, the removal packet and the status
 * in one step, the conversation projection and the verification refresh follow; a retry after a crash or a failed
 * later step continues where the removal stopped instead of failing because the member is already inactive.
 */
sealed interface GroupMemberRemovalProgress {
    /** Membership still has to remove the member. */
    data object NotStarted : GroupMemberRemovalProgress

    /** Membership removed the member, the participant list still shows them. */
    data class Committed(
        val removal: GroupMemberRemovalResult
    ) : GroupMemberRemovalProgress

    /** Removed and projected; only the verification refresh may be missing. */
    data object Projected : GroupMemberRemovalProgress

    companion object {
        /**
         * A committed removal without an owner-side membership row is only proven by the participant row; without
         * it the contact was never a member and the removal must fail as before.
         */
        fun of(committed: GroupCommittedMemberRemoval?, isParticipant: Boolean): GroupMemberRemovalProgress =
            when {
                committed == null -> NotStarted
                isParticipant -> Committed(committed.result)
                committed.isRecorded -> Projected
                else -> NotStarted
            }
    }
}
