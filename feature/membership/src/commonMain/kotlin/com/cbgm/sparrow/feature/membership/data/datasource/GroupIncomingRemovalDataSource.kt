package com.cbgm.sparrow.feature.membership.data.datasource

import com.cbgm.sparrow.data.database.dao.GroupMembershipDao
import com.cbgm.sparrow.data.database.dao.GroupSecurityDao
import com.cbgm.sparrow.data.database.entity.GroupMembershipEntity
import com.cbgm.sparrow.feature.membership.crypto.GroupKeyStore
import com.cbgm.sparrow.feature.membership.data.GroupMembershipEvent
import com.cbgm.sparrow.feature.membership.data.GroupMembershipStateMachine
import com.cbgm.sparrow.feature.membership.data.model.GroupMembershipStatus
import com.cbgm.sparrow.feature.membership.data.model.isGroupAdminRole
import com.cbgm.sparrow.feature.membership.data.policy.PendingGroupRemovalPolicy
import com.cbgm.sparrow.feature.membership.data.protocol.GroupMembershipPacketProtocol
import com.cbgm.sparrow.protocol.identity.LocalPublicIdentityProvider
import com.cbgm.sparrow.protocol.packet.GroupMemberRemovedPacket

/** Membership alone validates removal authority and advances its persisted membership state. */
internal class GroupIncomingRemovalDataSource(
    private val groupMembershipDao: GroupMembershipDao,
    private val groupSecurityDao: GroupSecurityDao,
    private val packetProtocol: GroupMembershipPacketProtocol,
    private val securityStore: GroupSecurityStoreDataSource,
    private val groupKeyStore: GroupKeyStore,
    private val localPublicIdentityProvider: LocalPublicIdentityProvider
) {
    suspend fun authorize(
        packet: GroupMemberRemovedPacket,
        senderContactId: String
    ): Boolean {
        if (packet.epoch == GroupMemberRemovedPacket.PENDING_INVITATION_EPOCH) {
            val membership = groupMembershipDao.findBySourceInvitationId(packet.invitationId)
            val signingKey = PendingGroupRemovalPolicy.signingKey(packet, senderContactId, membership)
                ?: return false
            packetProtocol.verifyMemberRemoved(packet, signingKey).getOrThrow()
            return true
        }
        val state = groupSecurityDao.findState(packet.groupId)
            ?: error("Group security state was not found")
        val authority = groupSecurityDao.findMemberKey(packet.groupId, state.currentEpoch, senderContactId)
            ?: error("Group removal sender is not part of the current epoch")
        check(authority.role.isGroupAdminRole()) { "Group update sender is not an admin" }
        val expectedSigningKey = authority.signingPublicKey
        packetProtocol.verifyMemberRemoved(packet, expectedSigningKey).getOrThrow()
        val active = securityStore.findState(packet.groupId)
        val localSigningPublicKey = localPublicIdentityProvider.getLocalPublicIdentity().getOrThrow().signingPublicKey
        if (active != null) {
            check(packet.epoch > active.currentEpoch) { "Group removal must reference a later epoch" }
            check(active.localSigningPublicKey.contentEquals(localSigningPublicKey)) {
                "Local group identity does not match the current security state"
            }
        }
        check(packet.removedMemberSigningPublicKey.contentEquals(localSigningPublicKey)) {
            "Group removal targets a different member"
        }
        return true
    }

    suspend fun complete(packet: GroupMemberRemovedPacket) {
        groupKeyStore.deleteGroup(packet.groupId)
        securityStore.deleteGroup(packet.groupId)
        val membership: GroupMembershipEntity =
            groupMembershipDao.findBySourceInvitationId(packet.invitationId) ?: return
        if (membership.status == GroupMembershipStatus.REMOVED.name) return
        groupMembershipDao.updateStatus(
            membershipId = membership.membershipId,
            expectedStatus = membership.status,
            newStatus = GroupMembershipStateMachine.transition(
                membership.status,
                GroupMembershipEvent.REMOVE
            ).name,
            updatedAt = packet.removedAtEpochMilliseconds
        )
    }
}
