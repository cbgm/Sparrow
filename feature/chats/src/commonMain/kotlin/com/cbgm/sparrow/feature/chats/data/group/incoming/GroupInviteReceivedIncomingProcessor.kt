package com.cbgm.sparrow.feature.chats.data.group.incoming

import com.cbgm.sparrow.core.protocol.packet.GroupInviteReceivedPacket
import com.cbgm.sparrow.data.database.dao.GroupInvitationDao
import com.cbgm.sparrow.feature.chats.data.group.invitation.GroupInvitationDirection
import com.cbgm.sparrow.feature.chats.data.group.invitation.GroupInvitationStatus
import com.cbgm.sparrow.feature.chats.data.group.invitation.resolveInvitationUpdatedAt
import com.cbgm.sparrow.feature.chats.data.group.membership.GroupMembershipEvent
import com.cbgm.sparrow.feature.chats.data.group.membership.GroupMembershipIdentity
import com.cbgm.sparrow.feature.chats.data.group.membership.GroupMembershipStateMachine
import com.cbgm.sparrow.feature.chats.data.group.protocol.GroupMembershipPacketProtocol
import com.cbgm.sparrow.feature.chats.data.group.verification.GroupVerificationCoordinator

internal class GroupInviteReceivedIncomingProcessor(
    private val groupInvitationDao: GroupInvitationDao,
    private val membershipPacketProtocol: GroupMembershipPacketProtocol,
    private val identity: GroupMembershipIdentity,
    private val groupVerificationCoordinator: GroupVerificationCoordinator
) {
    suspend fun process(
        memberContactId: String,
        packet: GroupInviteReceivedPacket,
        receivedAtEpochMilliseconds: Long
    ): Result<Unit> =
        runCatching {
            val invitation =
                groupInvitationDao.findByInvitationId(packet.invitationId)
                    ?: return@runCatching
            check(invitation.direction == GroupInvitationDirection.OUTGOING.name) {
                "Invite receipt does not belong to an outgoing invitation"
            }
            check(invitation.groupId == packet.groupId) { "Invite receipt uses the wrong group" }
            check(invitation.contactId == memberContactId) { "Invite receipt came from the wrong contact" }
            check(invitation.challenge.contentEquals(packet.challenge)) { "Invite receipt challenge does not match" }
            check(receivedAtEpochMilliseconds <= invitation.expiresAtEpochMilliseconds) {
                "Group invitation has expired"
            }

            membershipPacketProtocol.verifyInviteReceived(packet).getOrThrow()
            identity.ensureSigningIdentityMatches(memberContactId, packet.memberSigningPublicKey)
            if (invitation.status != GroupInvitationStatus.INVITE_SENT.name) return@runCatching

            val updated =
                groupInvitationDao.updateStatus(
                    invitationId = invitation.invitationId,
                    expectedStatus = GroupInvitationStatus.INVITE_SENT.name,
                    newStatus =
                        GroupMembershipStateMachine
                            .transition(
                                GroupInvitationStatus.INVITE_SENT.name,
                                GroupMembershipEvent.INVITE_RECEIVED
                            ).name,
                    updatedAt =
                        resolveInvitationUpdatedAt(
                            createdAtEpochMilliseconds = invitation.createdAtEpochMilliseconds,
                            candidateAtEpochMilliseconds =
                                maxOf(
                                    packet.receivedAtEpochMilliseconds,
                                    receivedAtEpochMilliseconds
                                )
                        )
                )
            check(updated == 1) { "Group invitation changed while the receipt was applied" }
            groupVerificationCoordinator.onOwnedMembershipChanged(packet.groupId).getOrThrow()
        }
}
