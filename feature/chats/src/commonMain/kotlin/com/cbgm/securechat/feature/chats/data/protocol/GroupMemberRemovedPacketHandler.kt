package com.cbgm.securechat.feature.chats.data.protocol

import com.cbgm.securechat.core.protocol.handler.IncomingPacketContext
import com.cbgm.securechat.core.protocol.handler.TypedProtocolPacketHandler
import com.cbgm.securechat.core.protocol.identity.LocalPublicIdentityProvider
import com.cbgm.securechat.core.protocol.packet.GroupMemberRemovedPacket
import com.cbgm.securechat.core.protocol.packet.SecureChatPacket
import com.cbgm.securechat.data.database.dao.ChatDao
import com.cbgm.securechat.data.database.dao.ContactDao
import com.cbgm.securechat.data.database.dao.GroupInvitationDao
import com.cbgm.securechat.data.database.dao.GroupVerificationDao
import com.cbgm.securechat.feature.chats.data.invitation.GroupInvitationStatus
import com.cbgm.securechat.feature.chats.data.message.GroupMembershipMessageFactory
import com.cbgm.securechat.feature.chats.data.security.GroupInvitationManager
import com.cbgm.securechat.feature.chats.data.security.GroupSecurityManager

class GroupMemberRemovedPacketHandler(
    private val chatDao: ChatDao,
    private val contactDao: ContactDao,
    private val groupInvitationDao: GroupInvitationDao,
    private val groupVerificationDao: GroupVerificationDao,
    private val localPublicIdentityProvider: LocalPublicIdentityProvider,
    private val groupInvitationManager: GroupInvitationManager,
    private val groupSecurityManager: GroupSecurityManager
) : TypedProtocolPacketHandler {
    override fun canHandle(packet: SecureChatPacket): Boolean = packet is GroupMemberRemovedPacket

    override suspend fun handle(
        context: IncomingPacketContext,
        packet: SecureChatPacket
    ): Result<Unit> =
        runCatching {
            val removal =
                packet as? GroupMemberRemovedPacket
                    ?: error("GroupMemberRemovedPacketHandler received an incompatible packet")
            val authorityIdentity =
                contactDao.findPublicIdentityByContactId(context.contactId)
                    ?: error("Group admin identity was not found")
            groupInvitationManager
                .verifyMemberRemoved(
                    packet = removal,
                    expectedOwnerSigningPublicKey = authorityIdentity.signingPublicKey
                ).getOrThrow()

            val invitation = groupInvitationDao.findByInvitationId(removal.invitationId)
            if (removal.epoch == GroupMemberRemovedPacket.PENDING_INVITATION_EPOCH) {
                val pending = invitation ?: error("Removed group invitation was not found")
                check(pending.groupId == removal.groupId) { "Group removal references the wrong group" }
                check(pending.contactId == context.contactId) {
                    "Pending group removal came from a contact that is not the inviter"
                }
                check(pending.challenge.contentEquals(removal.challenge)) {
                    "Group removal invitation challenge does not match"
                }
                check(
                    pending.status == GroupInvitationStatus.AWAITING_ACCEPTANCE.name ||
                        pending.status == GroupInvitationStatus.JOIN_SENT.name
                ) {
                    "An installed group key requires an epoch-advancing removal"
                }
            } else {
                groupSecurityManager
                    .requireRemoteAdmin(
                        groupId = removal.groupId,
                        contactId = context.contactId,
                        signingPublicKey = authorityIdentity.signingPublicKey
                    ).getOrThrow()
            }

            val isLocallyHidden =
                chatDao.hasMessageWithTransportMode(
                    conversationId = removal.groupId,
                    transportMode = GroupMembershipMessageFactory.LOCAL_CONVERSATION_DELETED_TRANSPORT_MODE
                )
            val localIdentity = localPublicIdentityProvider.getLocalPublicIdentity().getOrThrow()
            groupSecurityManager
                .removeLocalMembership(
                    packet = removal,
                    ownerContactId = context.contactId,
                    localSigningPublicKey = localIdentity.signingPublicKey
                ).getOrThrow()

            if (!isLocallyHidden) {
                chatDao.applyLocalGroupRemoval(
                    if (removal.reason == GroupMemberRemovedPacket.REASON_MEMBER_LEFT) {
                        GroupMembershipMessageFactory.localMembershipLeft(
                            conversationId = removal.groupId,
                            invitationId = removal.invitationId,
                            epoch = removal.epoch,
                            createdAtEpochMilliseconds = removal.removedAtEpochMilliseconds
                        )
                    } else {
                        GroupMembershipMessageFactory.localMembershipRemoved(
                            conversationId = removal.groupId,
                            invitationId = removal.invitationId,
                            epoch = removal.epoch,
                            createdAtEpochMilliseconds = removal.removedAtEpochMilliseconds
                        )
                    }
                )
            }
            invitation?.let { existing ->
                if (existing.status != GroupInvitationStatus.REMOVED.name) {
                    groupInvitationDao.updateStatus(
                        invitationId = existing.invitationId,
                        expectedStatus = existing.status,
                        newStatus = GroupInvitationStatus.REMOVED.name,
                        updatedAt = removal.removedAtEpochMilliseconds
                    )
                }
            }
            groupVerificationDao.deleteByGroupId(removal.groupId)
        }
}
