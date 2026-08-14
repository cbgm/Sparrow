package com.cbgm.sparrow.feature.chats.data.group.incoming.handler

import com.cbgm.sparrow.core.protocol.handler.IncomingPacketContext
import com.cbgm.sparrow.core.protocol.packet.GroupConversationDeletedPacket
import com.cbgm.sparrow.core.protocol.packet.SparrowPacket
import com.cbgm.sparrow.data.database.dao.ChatDao
import com.cbgm.sparrow.data.database.dao.ContactDao
import com.cbgm.sparrow.data.database.dao.GroupInvitationDao
import com.cbgm.sparrow.data.database.dao.GroupVerificationDao
import com.cbgm.sparrow.feature.chats.data.group.invitation.GroupInvitationStatus
import com.cbgm.sparrow.feature.chats.data.group.membership.GroupMembershipEvent
import com.cbgm.sparrow.feature.chats.data.group.membership.GroupMembershipStateMachine
import com.cbgm.sparrow.feature.chats.data.group.protocol.GroupMembershipPacketProtocol
import com.cbgm.sparrow.feature.chats.data.group.security.GroupSecurityManager

class GroupConversationDeletedPacketHandler(
    private val chatDao: ChatDao,
    private val contactDao: ContactDao,
    private val groupInvitationDao: GroupInvitationDao,
    private val groupVerificationDao: GroupVerificationDao,
    private val membershipPacketProtocol: GroupMembershipPacketProtocol,
    private val groupSecurityManager: GroupSecurityManager
) : GroupPacketHandler {
    override fun canHandle(packet: SparrowPacket): Boolean = packet is GroupConversationDeletedPacket

    override suspend fun handle(
        context: IncomingPacketContext,
        packet: SparrowPacket
    ): Result<Unit> =
        runCatching {
            val deletion =
                packet as? GroupConversationDeletedPacket
                    ?: error("GroupConversationDeletedPacketHandler received an incompatible packet")
            val invitation =
                groupInvitationDao.findByInvitationId(deletion.invitationId)
                    ?: error("Deleted group invitation was not found")
            check(invitation.groupId == deletion.groupId) {
                "Group deletion references the wrong group"
            }
            check(invitation.contactId == context.contactId) {
                "Group deletion came from a contact that is not the group owner"
            }
            check(invitation.challenge.contentEquals(deletion.challenge)) {
                "Group deletion invitation challenge does not match"
            }
            check(deletion.deletedAtEpochMilliseconds >= invitation.createdAtEpochMilliseconds) {
                "Group deletion predates the invitation"
            }
            val ownerIdentity =
                contactDao.findPublicIdentityByContactId(context.contactId)
                    ?: error("Group owner identity was not found")
            membershipPacketProtocol
                .verifyConversationDeleted(
                    packet = deletion,
                    expectedOwnerSigningPublicKey = ownerIdentity.signingPublicKey
                ).getOrThrow()

            groupSecurityManager.deleteLocalGroup(deletion.groupId).getOrThrow()
            chatDao.deleteConversationParticipants(deletion.groupId)
            groupVerificationDao.deleteByGroupId(deletion.groupId)
            if (invitation.status != GroupInvitationStatus.GROUP_DELETED.name) {
                val updated =
                    groupInvitationDao.updateStatus(
                        invitationId = invitation.invitationId,
                        expectedStatus = invitation.status,
                        newStatus =
                            GroupMembershipStateMachine.transition(
                                invitation.status,
                                GroupMembershipEvent.GROUP_DELETED
                            ).name,
                        updatedAt = deletion.deletedAtEpochMilliseconds
                    )
                check(updated == 1) { "Group invitation changed while deletion was applied" }
            }
            chatDao.updateConversationTimestamp(
                conversationId = deletion.groupId,
                timestamp = deletion.deletedAtEpochMilliseconds
            )
        }
}
