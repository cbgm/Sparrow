package com.cbgm.sparrow.feature.conversationorchestration.data.group.invitation

import com.cbgm.sparrow.core.protocol.packet.GroupInviteDeclinedPacket
import com.cbgm.sparrow.feature.invite.domain.model.InvitationResponse
import com.cbgm.sparrow.feature.membership.data.GroupMembershipIdentity
import com.cbgm.sparrow.feature.membership.data.coordinator.GroupMembershipAdministrationCoordinator
import com.cbgm.sparrow.feature.membership.data.datasource.GroupMembershipAttemptDataSource
import com.cbgm.sparrow.feature.membership.data.datasource.GroupMembershipProtocolDataSource
import com.cbgm.sparrow.feature.membership.data.model.GroupMembershipStatus

internal class GroupInviteDeclinedIncomingProcessorImpl(
    private val membershipPacketProtocol: GroupMembershipProtocolDataSource,
    private val membershipAttempts: GroupMembershipAttemptDataSource,
    private val identity: GroupMembershipIdentity,
    private val administration: GroupMembershipAdministrationCoordinator
) {
    suspend fun process(
        memberContactId: String,
        packet: GroupInviteDeclinedPacket,
        receivedAtEpochMilliseconds: Long
    ): Result<InvitationResponse?> =
        runCatching {
            val membership =
                membershipAttempts.findBySourceInvitationId(packet.invitationId)
                    ?: return@runCatching null
            check(membership.groupId == packet.groupId) { "Decline uses the wrong group" }
            check(membership.contactId == memberContactId) { "Decline came from the wrong contact" }
            check(membership.challenge.contentEquals(packet.challenge)) { "Decline challenge does not match" }
            membershipPacketProtocol.verifyDecline(packet).getOrThrow()
            identity.ensureSigningIdentityMatches(memberContactId, packet.memberSigningPublicKey)

            if (
                membership.status == GroupMembershipStatus.WELCOME_SENT ||
                membership.status == GroupMembershipStatus.ACTIVE
            ) {
                administration
                    .removeDepartingMember(
                        groupId = packet.groupId,
                        contactId = memberContactId
                    ).getOrThrow()
                return@runCatching null
            }

            check(membership.status == GroupMembershipStatus.STAGED) {
                "Group membership cannot be declined from status ${membership.status}"
            }

            membershipAttempts.deleteAttempt(packet.invitationId).getOrThrow()
            membershipAttempts.refreshOwnedMembership(packet.groupId).getOrThrow()
            InvitationResponse.DECLINED
        }
}
