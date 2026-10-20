package com.cbgm.sparrow.feature.conversationorchestration.lifecycle.domain.command

import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.WorkflowCommand
import com.cbgm.sparrow.feature.identity.domain.model.ApprovedIdentityReconnection
import com.cbgm.sparrow.feature.identity.domain.model.IdentityResult
import com.cbgm.sparrow.feature.invite.domain.model.InvitationResult

/**
 * Every direct chat lifecycle job orchestration starts internally, from a runtime observer or another workflow.
 * Submit one with `engine.run(command)`; the engine finds the workflow that handles it.
 */
sealed interface LifecycleCommand : WorkflowCommand {
    /** A direct message found no usable authorization; the contact must authorize the chat again. */
    data class RequestReauthorization(
        val contactId: String
    ) : LifecycleCommand

    /** Ends the contact's current direct authorization in the way its exchange phase requires. */
    data class RevokePeerExchange(
        val contactId: String
    ) : LifecycleCommand

    /** Invites the contact to a direct conversation unless an authorized conversation already exists. */
    data class StartDirectInvitation(
        val contactId: String
    ) : LifecycleCommand

    /** A persisted contact block revokes the direct authorization, also for blocks recorded before startup. */
    data class HandleContactBlocked(
        val contactId: String
    ) : LifecycleCommand

    /** The user answered a direct invitation, or the contact declined ours. */
    data class HandleDirectInvitationResult(
        val result: InvitationResult
    ) : LifecycleCommand

    /** Identity reported a new state of a direct invitation exchange. */
    data class HandleIdentityResult(
        val result: IdentityResult
    ) : LifecycleCommand

    /**
     * Records a verified incoming direct invitation, or rejects it for a blocked or unwanted sender.
     * Only the persisted Identity result path declines ([declineIfRejected]); the packet path may race it and
     * must not close the same exchange twice.
     */
    data class RecordIncomingDirectInvitation(
        val result: IdentityResult,
        val peerDisplayName: String?,
        val declineIfRejected: Boolean
    ) : LifecycleCommand

    /** On startup, replays an accepted incoming direct invitation whose exchange is still open. */
    data class RecoverAcceptedDirectInvitation(
        val result: InvitationResult
    ) : LifecycleCommand

    /** Restart reconciliation of an established exchange whose chat may not exist yet. */
    data class RecoverAuthorizedConversation(
        val result: IdentityResult
    ) : LifecycleCommand

    /**
     * The original invitation came from the contact's NEW identity; its keys, challenge and timestamps were
     * staged only after signature verification and the user accepted them. Reusing that challenge sends the
     * normal signed acceptance, so the sender's outgoing exchange completes without a second invitation.
     */
    data class AcceptApprovedIdentityChange(
        val approval: ApprovedIdentityReconnection
    ) : LifecycleCommand

    /**
     * Resumes a one-tap identity-change approval in its original invitation flow. A member-side group offer
     * stays in the inbox for ordinary acceptance; an owner-side signed JOIN resumes its membership; direct
     * invitations resume their original challenge while it is still valid.
     */
    data class StartRecoveryInvitation(
        val approval: ApprovedIdentityReconnection
    ) : LifecycleCommand
}
