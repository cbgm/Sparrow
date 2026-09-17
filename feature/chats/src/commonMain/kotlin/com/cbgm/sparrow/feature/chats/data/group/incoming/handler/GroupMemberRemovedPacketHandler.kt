package com.cbgm.sparrow.feature.chats.data.group.incoming.handler

import com.cbgm.sparrow.core.protocol.handler.IncomingPacketContext
import com.cbgm.sparrow.core.protocol.identity.LocalPublicIdentityProvider
import com.cbgm.sparrow.core.protocol.packet.GroupMemberRemovedPacket
import com.cbgm.sparrow.core.protocol.packet.SparrowPacket
import com.cbgm.sparrow.data.database.dao.ChatDao
import com.cbgm.sparrow.data.database.dao.ContactDao
import com.cbgm.sparrow.data.database.dao.GroupMembershipDao
import com.cbgm.sparrow.data.database.dao.GroupVerificationDao
import com.cbgm.sparrow.data.database.entity.GroupMembershipEntity
import com.cbgm.sparrow.feature.chats.data.group.mapper.GroupMembershipMessageFactory
import com.cbgm.sparrow.feature.chats.data.group.protocol.GroupMembershipPacketProtocol
import com.cbgm.sparrow.feature.chats.data.group.security.GroupSecurityManager
import com.cbgm.sparrow.feature.membership.data.GroupMembershipEvent
import com.cbgm.sparrow.feature.membership.data.GroupMembershipStateMachine
import com.cbgm.sparrow.feature.membership.data.model.GroupMembershipStatus

class GroupMemberRemovedPacketHandler(
    private val chatDao: ChatDao,
    private val contactDao: ContactDao,
    private val groupMembershipDao: GroupMembershipDao,
    private val groupVerificationDao: GroupVerificationDao,
    private val localPublicIdentityProvider: LocalPublicIdentityProvider,
    private val membershipPacketProtocol: GroupMembershipPacketProtocol,
    private val groupSecurityManager: GroupSecurityManager
) : GroupPacketHandler {
    override fun canHandle(packet: SparrowPacket): Boolean =
        packet is GroupMemberRemovedPacket

    override suspend fun handle(
        context: IncomingPacketContext,
        packet: SparrowPacket
    ): Result<Unit> =
        runCatching {
            val removal = packet.requireRemovalPacket()
            val membership = groupMembershipDao.findBySourceInvitationId(removal.invitationId)
            if (!validateRemoval(context, removal, membership)) return@runCatching

            val wasLocallyHidden = isLocallyHidden(removal.groupId)
            removeLocalSecurityState(context, removal)
            if (!wasLocallyHidden) {
                applyLocalRemovalMessage(removal)
            }

            markMembershipRemoved(membership, removal)
            groupVerificationDao.deleteByGroupId(removal.groupId)
        }

    private fun SparrowPacket.requireRemovalPacket(): GroupMemberRemovedPacket =
        this as? GroupMemberRemovedPacket
            ?: error("GroupMemberRemovedPacketHandler received an incompatible packet")

    private suspend fun validateRemoval(
        context: IncomingPacketContext,
        packet: GroupMemberRemovedPacket,
        membership: GroupMembershipEntity?
    ): Boolean {
        if (packet.epoch == GroupMemberRemovedPacket.PENDING_INVITATION_EPOCH) {
            val authorityIdentity =
                contactDao.findPublicIdentityByContactId(context.contactId)
                    ?: error("Pending group membership owner identity was not found")
            membershipPacketProtocol
                .verifyMemberRemoved(
                    packet = packet,
                    expectedOwnerSigningPublicKey = authorityIdentity.signingPublicKey
                ).getOrThrow()
            return validatePendingInvitationRemoval(context, packet, membership)
        }

        val authorityMemberKey =
            groupSecurityManager
                .findRemoteMemberKey(packet.groupId, context.contactId)
                .getOrThrow()
                ?: error("Group removal sender is not part of the current epoch")
        groupSecurityManager
            .requireRemoteAdmin(
                groupId = packet.groupId,
                contactId = context.contactId,
                signingPublicKey = authorityMemberKey.signingPublicKey
            ).getOrThrow()
        membershipPacketProtocol
            .verifyMemberRemoved(
                packet = packet,
                expectedOwnerSigningPublicKey = authorityMemberKey.signingPublicKey
            ).getOrThrow()
        return true
    }

    private fun validatePendingInvitationRemoval(
        context: IncomingPacketContext,
        packet: GroupMemberRemovedPacket,
        membership: GroupMembershipEntity?
    ): Boolean {
        val pending = membership ?: return false
        check(pending.groupId == packet.groupId) { "Group removal references the wrong group" }
        check(pending.contactId == context.contactId) {
            "Pending group removal came from a contact that is not the inviter"
        }
        check(pending.challenge.contentEquals(packet.challenge)) {
            "Group removal membership challenge does not match"
        }
        if (pending.status == GroupMembershipStatus.REMOVED.name) return false
        check(
            pending.status == GroupMembershipStatus.STAGED.name ||
                pending.status == GroupMembershipStatus.JOIN_REQUEST_SENT.name
        ) {
            "An installed group key requires an epoch-advancing removal"
        }
        return true
    }

    private suspend fun isLocallyHidden(groupId: String): Boolean =
        chatDao.hasMessageWithTransportMode(
            conversationId = groupId,
            transportMode = GroupMembershipMessageFactory.LOCAL_CONVERSATION_DELETED_TRANSPORT_MODE
        )

    private suspend fun removeLocalSecurityState(
        context: IncomingPacketContext,
        packet: GroupMemberRemovedPacket
    ) {
        val localIdentity = localPublicIdentityProvider.getLocalPublicIdentity().getOrThrow()
        groupSecurityManager
            .removeLocalMembership(
                packet = packet,
                ownerContactId = context.contactId,
                localSigningPublicKey = localIdentity.signingPublicKey
            ).getOrThrow()
    }

    private suspend fun applyLocalRemovalMessage(packet: GroupMemberRemovedPacket) {
        val message =
            if (packet.reason == GroupMemberRemovedPacket.REASON_MEMBER_LEFT) {
                GroupMembershipMessageFactory.localMembershipLeft(
                    conversationId = packet.groupId,
                    invitationId = packet.invitationId,
                    epoch = packet.epoch,
                    createdAtEpochMilliseconds = packet.removedAtEpochMilliseconds
                )
            } else {
                GroupMembershipMessageFactory.localMembershipRemoved(
                    conversationId = packet.groupId,
                    invitationId = packet.invitationId,
                    epoch = packet.epoch,
                    createdAtEpochMilliseconds = packet.removedAtEpochMilliseconds
                )
            }
        chatDao.applyLocalGroupRemoval(message)
    }

    private suspend fun markMembershipRemoved(
        membership: GroupMembershipEntity?,
        packet: GroupMemberRemovedPacket
    ) {
        val existing = membership ?: return
        if (existing.status == GroupMembershipStatus.REMOVED.name) return

        groupMembershipDao.updateStatus(
            membershipId = existing.membershipId,
            expectedStatus = existing.status,
            newStatus =
                GroupMembershipStateMachine.transition(
                    existing.status,
                    GroupMembershipEvent.REMOVE
                ).name,
            updatedAt = packet.removedAtEpochMilliseconds
        )
    }
}
