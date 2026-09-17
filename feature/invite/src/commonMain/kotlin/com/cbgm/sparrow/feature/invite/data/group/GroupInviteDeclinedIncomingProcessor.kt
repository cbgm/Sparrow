package com.cbgm.sparrow.feature.invite.data.group

import com.cbgm.sparrow.core.protocol.packet.GroupInviteDeclinedPacket
import com.cbgm.sparrow.data.database.dao.InvitationDao
import com.cbgm.sparrow.feature.invite.domain.model.InvitationPayloadType
import com.cbgm.sparrow.feature.membership.data.GroupMembershipIdentity
import com.cbgm.sparrow.feature.membership.data.coordinator.GroupMembershipAdministrationCoordinator
import com.cbgm.sparrow.feature.membership.data.datasource.GroupMembershipAttemptDataSource
import com.cbgm.sparrow.feature.membership.data.datasource.GroupMembershipProtocolDataSource
import com.cbgm.sparrow.feature.membership.data.model.GroupMembershipStatus

internal class GroupInviteDeclinedIncomingProcessor(
    private val invitationDao: InvitationDao,
    private val membershipPacketProtocol: GroupMembershipProtocolDataSource,
    private val membershipAttempts: GroupMembershipAttemptDataSource,
    private val identity: GroupMembershipIdentity,
    private val administration: GroupMembershipAdministrationCoordinator
) {
    suspend fun process(
        memberContactId: String,
        packet: GroupInviteDeclinedPacket,
        receivedAtEpochMilliseconds: Long
    ): Result<Unit> =
        runCatching {
            val invitation = invitationDao.findById(packet.invitationId) ?: return@runCatching
            check(invitation.payloadType == InvitationPayloadType.GROUP.name) { "Invitation is not a group invitation" }
            check(invitation.payloadId == packet.groupId) { "Decline uses the wrong group" }
            check(invitation.peerId == memberContactId) { "Decline came from the wrong contact" }

            val membership =
                membershipAttempts.findBySourceInvitationId(packet.invitationId)
                    ?: return@runCatching
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
                return@runCatching
            }

            check(invitation.status == INVITATION_STATUS_PENDING) {
                "Group invitation cannot be declined from status ${invitation.status}"
            }
            check(membership.status == GroupMembershipStatus.STAGED) {
                "Group membership cannot be declined from status ${membership.status}"
            }

            val updatedAt = maxOf(invitation.createdAtEpochMilliseconds, receivedAtEpochMilliseconds)
            val updated =
                invitationDao.updateStatus(
                    invitationId = invitation.invitationId,
                    expectedStatus = INVITATION_STATUS_PENDING,
                    newStatus = INVITATION_STATUS_DECLINED,
                    updatedAt = updatedAt
                )
            check(updated == 1) { "Group invitation changed while the decline was applied" }
            membershipAttempts.deleteAttempt(packet.invitationId).getOrThrow()
            membershipAttempts.refreshOwnedMembership(packet.groupId).getOrThrow()
        }

    private companion object {
        const val INVITATION_STATUS_PENDING = "PENDING"
        const val INVITATION_STATUS_DECLINED = "DECLINED"
    }
}
