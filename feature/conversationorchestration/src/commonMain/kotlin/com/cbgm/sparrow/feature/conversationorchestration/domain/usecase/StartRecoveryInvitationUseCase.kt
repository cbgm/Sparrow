package com.cbgm.sparrow.feature.conversationorchestration.domain.usecase

import com.cbgm.sparrow.core.time.SystemClock
import com.cbgm.sparrow.feature.conversationorchestration.domain.port.ConversationPort
import com.cbgm.sparrow.feature.conversationorchestration.domain.workflow.ConversationFlowHandler
import com.cbgm.sparrow.feature.identity.domain.model.ApprovedIdentityReconnection
import com.cbgm.sparrow.feature.membership.domain.usecase.GetMembershipHandshakeUseCase

/** Resume a one-tap identity-change approval in its original invitation flow.
 * A member-side group offer stays in Mailbox for ordinary group acceptance;
 * an owner-side signed group JOIN resumes its existing membership/welcome.
 * Direct invitations resume their original challenge when still valid. */
class StartRecoveryInvitationUseCase internal constructor(
    private val flowHandler: ConversationFlowHandler,
    private val conversations: ConversationPort,
    private val getMembershipHandshake: GetMembershipHandshakeUseCase
) {
    suspend operator fun invoke(approval: ApprovedIdentityReconnection): Result<Unit> = runCatching {
        val peerId = approval.peerId
        // A group identity approval is never an invitation to create a direct
        // conversation. Distinguish the staged owner JOIN from a member offer.
        val pendingGroup = getMembershipHandshake(approval.approvalId).getOrThrow()
        if (pendingGroup != null) {
            check(pendingGroup.peerId == peerId) {
                "Approved group identity does not match its inviting contact"
            }
            if (pendingGroup.ownerSigningPublicKey == null &&
                pendingGroup.ownerEncryptionPublicKey == null
            ) {
                // Owner-side: this approval belongs to a signed JOIN from a
                // member with replaced keys. Complete its existing membership,
                // not an unrelated direct invitation or a newly created chat.
                flowHandler.resumeApprovedGroupJoin(pendingGroup.sourceId, peerId).getOrThrow()
            }
            // Member-side: the signed group offer is still in Mailbox for normal
            // group acceptance. No additional direct invitation is needed.
            return@runCatching
        }
        // A withdrawn group membership must never become a DIRECT recovery merely
        // because its handshake was deleted before this persistent worker ran.
        // Group source IDs are minted with this prefix by Membership.
        if (approval.approvalId.startsWith("group-membership-")) return@runCatching
        // A missing chat on either side gets a shell. This does not grant transport
        // authorization or flush queued message source material.
        conversations.getOrCreateConversation(peerId).getOrThrow()
        val inviteExpiresAt = approval.originalInviteExpiresAtEpochMilliseconds
        if (approval.originalInviteChallenge != null &&
            approval.originalInviteCreatedAtEpochMilliseconds != null &&
            inviteExpiresAt != null &&
            approval.originalInviterEncryptionPublicKey != null &&
            approval.originalInviterSigningPublicKey != null &&
            inviteExpiresAt > SystemClock.nowEpochMilliseconds()
        ) {
            flowHandler.acceptApprovedOriginalIdentityChange(approval).getOrThrow()
        } else {
            // The source was a changed-key acceptance, an already expired original
            // invitation, or an old v52 approval without persisted challenge.
            flowHandler.requestReauthorization(peerId).getOrThrow()
        }
    }
}
