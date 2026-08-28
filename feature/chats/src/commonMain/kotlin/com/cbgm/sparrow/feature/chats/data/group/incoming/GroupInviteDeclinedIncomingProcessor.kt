package com.cbgm.sparrow.feature.chats.data.group.incoming

import com.cbgm.sparrow.core.protocol.packet.GroupInviteDeclinedPacket
import com.cbgm.sparrow.data.database.dao.GroupInvitationDao
import com.cbgm.sparrow.feature.chats.data.group.invitation.GroupInvitationStatus
import com.cbgm.sparrow.feature.chats.data.group.invitation.resolveInvitationUpdatedAt
import com.cbgm.sparrow.feature.chats.data.group.membership.GroupMembershipAdministrationCoordinator
import com.cbgm.sparrow.feature.chats.data.group.membership.GroupMembershipEvent
import com.cbgm.sparrow.feature.chats.data.group.membership.GroupMembershipIdentity
import com.cbgm.sparrow.feature.chats.data.group.membership.GroupMembershipStateMachine
import com.cbgm.sparrow.feature.chats.data.group.protocol.GroupMembershipPacketProtocol
import com.cbgm.sparrow.feature.chats.data.group.verification.GroupVerificationCoordinator

internal class GroupInviteDeclinedIncomingProcessor(
    private val groupInvitationDao: GroupInvitationDao,
    private val membershipPacketProtocol: GroupMembershipPacketProtocol,
    private val identity: GroupMembershipIdentity,
    private val administration: GroupMembershipAdministrationCoordinator,
    private val groupVerificationCoordinator: GroupVerificationCoordinator
) {
    suspend fun process(
        memberContactId: String,
        packet: GroupInviteDeclinedPacket,
        receivedAtEpochMilliseconds: Long
    ): Result<Unit> =
        runCatching {
            val invitation =
                groupInvitationDao.findByInvitationId(packet.invitationId)
                    ?: error("Group invitation was not found")
            check(invitation.groupId == packet.groupId) { "Decline uses the wrong group" }
            check(invitation.contactId == memberContactId) { "Decline came from the wrong contact" }
            check(invitation.challenge.contentEquals(packet.challenge)) { "Decline challenge does not match" }
            membershipPacketProtocol.verifyDecline(packet).getOrThrow()
            identity.ensureSigningIdentityMatches(memberContactId, packet.memberSigningPublicKey)

            if (
                invitation.status == GroupInvitationStatus.DECLINED.name ||
                invitation.status == GroupInvitationStatus.REMOVED.name
            ) {
                return@runCatching
            }
            if (
                invitation.status == GroupInvitationStatus.WELCOME_SENT.name ||
                invitation.status == GroupInvitationStatus.ACTIVE.name
            ) {
                administration
                    .removeDepartingMember(
                        groupId = packet.groupId,
                        contactId = memberContactId
                    ).getOrThrow()
                return@runCatching
            }
            check(
                invitation.status == GroupInvitationStatus.INVITE_SENT.name ||
                    invitation.status == GroupInvitationStatus.INVITE_RECEIVED.name ||
                    invitation.status == GroupInvitationStatus.WAITING_FOR_IDENTITY.name ||
                    invitation.status == GroupInvitationStatus.IDENTITY_READY.name
            ) {
                "Group invitation cannot be declined after it was accepted"
            }
            val updated =
                groupInvitationDao.updateStatus(
                    invitationId = invitation.invitationId,
                    expectedStatus = invitation.status,
                    newStatus =
                        GroupMembershipStateMachine
                            .transition(invitation.status, GroupMembershipEvent.DECLINE)
                            .name,
                    updatedAt =
                        resolveInvitationUpdatedAt(
                            createdAtEpochMilliseconds = invitation.createdAtEpochMilliseconds,
                            candidateAtEpochMilliseconds = receivedAtEpochMilliseconds
                        )
                )
            check(updated == 1) { "Group invitation changed while the decline was applied" }
            groupVerificationCoordinator.onOwnedMembershipChanged(packet.groupId).getOrThrow()
        }
}
