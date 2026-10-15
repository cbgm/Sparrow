package com.cbgm.sparrow.feature.conversationorchestration.membership.domain.command

import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.WorkflowCommand
import com.cbgm.sparrow.feature.invite.domain.model.InvitationResult
import com.cbgm.sparrow.feature.membership.domain.model.MembershipResult

/**
 * Every membership job orchestration starts internally, from a runtime observer or another workflow.
 * Submit one with `engine.run(command)`; the engine finds the workflow that handles it.
 */
sealed interface MembershipCommand : WorkflowCommand {
    /** The admin's membership changed; its verification rows and members' snapshots must follow. */
    data class RefreshOwnedGroupVerification(
        val groupId: String
    ) : MembershipCommand

    /** The user answered an incoming group invitation. */
    data class HandleGroupInvitationResult(
        val result: InvitationResult
    ) : MembershipCommand

    /** Membership reported progress of an invited member; only the owner's perspective needs a reaction. */
    data class HandleMembershipResult(
        val result: MembershipResult
    ) : MembershipCommand

    /**
     * On startup, replays an accepted incoming group invitation whose membership handshake still exists.
     * Historical results of removed or superseded memberships are never reapplied.
     */
    data class RecoverAcceptedGroupInvitation(
        val result: InvitationResult
    ) : MembershipCommand

    /**
     * Continues an authenticated owner-side JOIN after the user approved the member's new keys.
     * [sourceId] is the original membership; a re-run after a crash reuses it and never creates a direct chat.
     */
    data class ResumeApprovedGroupJoin(
        val sourceId: String,
        val contactId: String
    ) : MembershipCommand
}
