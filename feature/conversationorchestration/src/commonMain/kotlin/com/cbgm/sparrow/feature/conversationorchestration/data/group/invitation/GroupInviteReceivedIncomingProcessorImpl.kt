package com.cbgm.sparrow.feature.conversationorchestration.data.group.invitation

import com.cbgm.sparrow.core.protocol.packet.GroupInviteReceivedPacket
import com.cbgm.sparrow.feature.membership.data.GroupMembershipIdentity
import com.cbgm.sparrow.feature.membership.data.datasource.GroupMembershipAttemptDataSource
import com.cbgm.sparrow.feature.membership.data.datasource.GroupMembershipProtocolDataSource
import com.cbgm.sparrow.feature.membership.data.model.GroupMembershipPerspective

internal class GroupInviteReceivedIncomingProcessorImpl(
    private val membershipPacketProtocol: GroupMembershipProtocolDataSource,
    private val membershipAttempts: GroupMembershipAttemptDataSource,
    private val identity: GroupMembershipIdentity
) {
    suspend fun process(
        memberContactId: String,
        packet: GroupInviteReceivedPacket,
        receivedAtEpochMilliseconds: Long
    ): Result<Unit> =
        runCatching {
            val membership =
                membershipAttempts.findBySourceInvitationId(packet.invitationId)
                    ?: return@runCatching

            check(membership.perspective == GroupMembershipPerspective.OWNER) {
                "Invite receipt does not belong to an owner-side membership attempt"
            }
            check(membership.groupId == packet.groupId) { "Invite receipt uses the wrong group" }
            check(membership.contactId == memberContactId) { "Invite receipt came from the wrong contact" }
            check(membership.challenge.contentEquals(packet.challenge)) { "Invite receipt challenge does not match" }

            membershipPacketProtocol.verifyInviteReceived(packet).getOrThrow()
            identity.ensureSigningIdentityMatches(memberContactId, packet.memberSigningPublicKey)
            membershipAttempts.refreshOwnedMembership(packet.groupId).getOrThrow()
        }
}
