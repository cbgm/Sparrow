package com.cbgm.sparrow.feature.membership.data.datasource

import com.cbgm.sparrow.core.crypto.group.GroupKeyStore
import com.cbgm.sparrow.core.protocol.identity.LocalPublicIdentityProvider
import com.cbgm.sparrow.core.protocol.packet.GroupMemberRemovedPacket
import com.cbgm.sparrow.data.database.dao.GroupMembershipDao
import com.cbgm.sparrow.data.database.dao.GroupSecurityDao
import com.cbgm.sparrow.data.database.entity.GroupMembershipEntity
import com.cbgm.sparrow.feature.membership.data.GroupMembershipEvent
import com.cbgm.sparrow.feature.membership.data.GroupMembershipStateMachine
import com.cbgm.sparrow.feature.membership.data.model.GroupMembershipStatus
import com.cbgm.sparrow.feature.membership.data.model.isGroupAdminRole
import com.cbgm.sparrow.feature.membership.data.protocol.GroupMembershipPacketProtocol

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
        senderContactId: String,
        pendingOwnerSigningPublicKey: ByteArray?
    ): Boolean {
        val membership = groupMembershipDao.findBySourceInvitationId(packet.invitationId)
        val expectedSigningKey =
            if (packet.epoch == GroupMemberRemovedPacket.PENDING_INVITATION_EPOCH) {
                pendingOwnerSigningPublicKey
                    ?: error("Pending group membership owner identity was not found")
            } else {
                val state = groupSecurityDao.findState(packet.groupId)
                    ?: error("Group security state was not found")
                val authority = groupSecurityDao.findMemberKey(
                    packet.groupId,
                    state.currentEpoch,
                    senderContactId
                ) ?: error("Group removal sender is not part of the current epoch")
                check(authority.role.isGroupAdminRole()) {
                    "Group update sender is not an admin"
                }
                authority.signingPublicKey
            }
        packetProtocol.verifyMemberRemoved(packet, expectedSigningKey).getOrThrow()
        if (packet.epoch > GroupMemberRemovedPacket.PENDING_INVITATION_EPOCH) {
            val state = securityStore.findState(packet.groupId)
            state?.let { active ->
                check(packet.epoch > active.currentEpoch) { "Group removal must reference a later epoch" }
                val localKey = localPublicIdentityProvider.getLocalPublicIdentity().getOrThrow().signingPublicKey
                check(active.localSigningPublicKey.contentEquals(localKey)) {
                    "Local group identity does not match the current security state"
                }
            }
            val localSigningPublicKey = localPublicIdentityProvider.getLocalPublicIdentity().getOrThrow().signingPublicKey
            check(packet.removedMemberSigningPublicKey.contentEquals(localSigningPublicKey)) {
                "Group removal targets a different member"
            }
        }
        if (packet.epoch != GroupMemberRemovedPacket.PENDING_INVITATION_EPOCH) return true
        val pending = membership ?: return false
        check(pending.groupId == packet.groupId) { "Group removal references the wrong group" }
        check(pending.contactId == senderContactId) {
            "Pending group removal came from a contact that is not the inviter"
        }
        check(pending.challenge.contentEquals(packet.challenge)) {
            "Group removal membership challenge does not match"
        }
        if (pending.status == GroupMembershipStatus.REMOVED.name) return false
        check(
            pending.status == GroupMembershipStatus.STAGED.name ||
                pending.status == GroupMembershipStatus.JOIN_REQUEST_SENT.name
        ) { "An installed group key requires an epoch-advancing removal" }
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
