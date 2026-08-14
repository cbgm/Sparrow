package com.cbgm.sparrow.feature.chats.data.group.incoming.handler

import com.cbgm.sparrow.core.protocol.handler.IncomingPacketContext
import com.cbgm.sparrow.core.protocol.identity.LocalPublicIdentityProvider
import com.cbgm.sparrow.core.protocol.packet.GroupMemberRemovedPacket
import com.cbgm.sparrow.core.protocol.packet.SparrowPacket
import com.cbgm.sparrow.data.database.dao.ChatDao
import com.cbgm.sparrow.data.database.dao.ContactDao
import com.cbgm.sparrow.data.database.dao.GroupInvitationDao
import com.cbgm.sparrow.data.database.dao.GroupVerificationDao
import com.cbgm.sparrow.data.database.entity.GroupInvitationEntity
import com.cbgm.sparrow.feature.chats.data.group.invitation.GroupInvitationStatus
import com.cbgm.sparrow.feature.chats.data.group.mapper.GroupMembershipMessageFactory
import com.cbgm.sparrow.feature.chats.data.group.membership.GroupMembershipEvent
import com.cbgm.sparrow.feature.chats.data.group.membership.GroupMembershipStateMachine
import com.cbgm.sparrow.feature.chats.data.group.protocol.GroupMembershipPacketProtocol
import com.cbgm.sparrow.feature.chats.data.group.security.GroupSecurityManager

class GroupMemberRemovedPacketHandler(
    private val chatDao: ChatDao,
    private val contactDao: ContactDao,
    private val groupInvitationDao: GroupInvitationDao,
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
            val invitation = groupInvitationDao.findByInvitationId(removal.invitationId)
            validateRemoval(context, removal, invitation)

            val wasLocallyHidden = isLocallyHidden(removal.groupId)
            removeLocalSecurityState(context, removal)
            if (!wasLocallyHidden) {
                applyLocalRemovalMessage(removal)
            }

            markInvitationRemoved(invitation, removal)
            groupVerificationDao.deleteByGroupId(removal.groupId)
        }

    private fun SparrowPacket.requireRemovalPacket(): GroupMemberRemovedPacket =
        this as? GroupMemberRemovedPacket
            ?: error("GroupMemberRemovedPacketHandler received an incompatible packet")

    private suspend fun validateRemoval(
        context: IncomingPacketContext,
        packet: GroupMemberRemovedPacket,
        invitation: GroupInvitationEntity?
    ) {
        if (packet.epoch == GroupMemberRemovedPacket.PENDING_INVITATION_EPOCH) {
            val authorityIdentity =
                contactDao.findPublicIdentityByContactId(context.contactId)
                    ?: error("Pending group invitation owner identity was not found")
            membershipPacketProtocol
                .verifyMemberRemoved(
                    packet = packet,
                    expectedOwnerSigningPublicKey = authorityIdentity.signingPublicKey
                ).getOrThrow()
            validatePendingInvitationRemoval(context, packet, invitation)
            return
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
    }

    private fun validatePendingInvitationRemoval(
        context: IncomingPacketContext,
        packet: GroupMemberRemovedPacket,
        invitation: GroupInvitationEntity?
    ) {
        val pending = invitation ?: error("Removed group invitation was not found")
        check(pending.groupId == packet.groupId) { "Group removal references the wrong group" }
        check(pending.contactId == context.contactId) {
            "Pending group removal came from a contact that is not the inviter"
        }
        check(pending.challenge.contentEquals(packet.challenge)) {
            "Group removal invitation challenge does not match"
        }
        check(
            pending.status == GroupInvitationStatus.AWAITING_ACCEPTANCE.name ||
                pending.status == GroupInvitationStatus.JOIN_SENT.name
        ) {
            "An installed group key requires an epoch-advancing removal"
        }
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

    private suspend fun markInvitationRemoved(
        invitation: GroupInvitationEntity?,
        packet: GroupMemberRemovedPacket
    ) {
        val existing = invitation ?: return
        if (existing.status == GroupInvitationStatus.REMOVED.name) return

        groupInvitationDao.updateStatus(
            invitationId = existing.invitationId,
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
