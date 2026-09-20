package com.cbgm.sparrow.feature.membership.data.datasource

import com.cbgm.sparrow.core.crypto.group.GroupKeyStore
import com.cbgm.sparrow.core.protocol.packet.GroupConversationDeletedPacket
import com.cbgm.sparrow.data.database.dao.GroupMembershipDao
import com.cbgm.sparrow.data.database.entity.GroupMembershipEntity
import com.cbgm.sparrow.feature.membership.data.GroupMembershipEvent
import com.cbgm.sparrow.feature.membership.data.GroupMembershipStateMachine
import com.cbgm.sparrow.feature.membership.data.model.GroupMembershipStatus
import com.cbgm.sparrow.feature.membership.data.protocol.GroupMembershipPacketProtocol

/** Validates and records deletion in Membership's OWN table. Chats cleanup is orchestrated separately. */
internal class GroupIncomingDeletionDataSource(
    private val groupMembershipDao: GroupMembershipDao,
    private val packetProtocol: GroupMembershipPacketProtocol,
    private val securityStore: GroupSecurityStoreDataSource,
    private val groupKeyStore: GroupKeyStore
) {
    suspend fun authorize(
        packet: GroupConversationDeletedPacket,
        ownerContactId: String,
        ownerSigningPublicKey: ByteArray
    ) {
        requireMembership(packet, ownerContactId)
        packetProtocol.verifyConversationDeleted(packet, ownerSigningPublicKey).getOrThrow()
    }

    suspend fun complete(packet: GroupConversationDeletedPacket, ownerContactId: String) {
        val membership = requireMembership(packet, ownerContactId)
        // The key and Membership security state are owned here, never by the Chats port.
        groupKeyStore.deleteGroup(packet.groupId)
        securityStore.deleteGroup(packet.groupId)
        if (membership.status == GroupMembershipStatus.GROUP_DELETED.name) return
        val updated = groupMembershipDao.updateStatus(
            membershipId = membership.membershipId,
            expectedStatus = membership.status,
            newStatus = GroupMembershipStateMachine.transition(
                membership.status,
                GroupMembershipEvent.GROUP_DELETED
            ).name,
            updatedAt = packet.deletedAtEpochMilliseconds
        )
        check(updated == 1) { "Group membership changed while deletion was applied" }
    }

    private suspend fun requireMembership(
        packet: GroupConversationDeletedPacket,
        ownerContactId: String
    ): GroupMembershipEntity {
        val membership = groupMembershipDao.findBySourceInvitationId(packet.invitationId)
            ?: error("Deleted group membership was not found")
        check(membership.groupId == packet.groupId) { "Group deletion references the wrong group" }
        check(membership.contactId == ownerContactId) {
            "Group deletion came from a contact that is not the group owner"
        }
        check(membership.challenge.contentEquals(packet.challenge)) {
            "Group deletion membership challenge does not match"
        }
        check(packet.deletedAtEpochMilliseconds >= membership.createdAtEpochMilliseconds) {
            "Group deletion predates the membership attempt"
        }
        return membership
    }
}
