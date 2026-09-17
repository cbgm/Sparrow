package com.cbgm.sparrow.feature.chats.data.group.incoming.handler

import com.cbgm.sparrow.core.protocol.handler.IncomingPacketContext
import com.cbgm.sparrow.core.protocol.packet.GroupConversationDeletedPacket
import com.cbgm.sparrow.core.protocol.packet.SparrowPacket
import com.cbgm.sparrow.data.database.dao.ChatDao
import com.cbgm.sparrow.data.database.dao.ContactDao
import com.cbgm.sparrow.data.database.dao.GroupMembershipDao
import com.cbgm.sparrow.data.database.dao.GroupVerificationDao
import com.cbgm.sparrow.feature.attachments.domain.usecase.DeleteConversationLocalAttachmentsUseCase
import com.cbgm.sparrow.feature.chats.data.group.protocol.GroupMembershipPacketProtocol
import com.cbgm.sparrow.feature.chats.data.group.security.GroupSecurityManager
import com.cbgm.sparrow.feature.membership.data.GroupMembershipEvent
import com.cbgm.sparrow.feature.membership.data.GroupMembershipStateMachine
import com.cbgm.sparrow.feature.membership.data.model.GroupMembershipStatus

class GroupConversationDeletedPacketHandler internal constructor(
    private val chatDao: ChatDao,
    private val contactDao: ContactDao,
    private val groupMembershipDao: GroupMembershipDao,
    private val groupVerificationDao: GroupVerificationDao,
    private val membershipPacketProtocol: GroupMembershipPacketProtocol,
    private val groupSecurityManager: GroupSecurityManager,
    private val deleteConversationLocalAttachments: DeleteConversationLocalAttachmentsUseCase
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
            val membership =
                groupMembershipDao.findBySourceInvitationId(deletion.invitationId)
                    ?: error("Deleted group membership was not found")
            check(membership.groupId == deletion.groupId) {
                "Group deletion references the wrong group"
            }
            check(membership.contactId == context.contactId) {
                "Group deletion came from a contact that is not the group owner"
            }
            check(membership.challenge.contentEquals(deletion.challenge)) {
                "Group deletion membership challenge does not match"
            }
            check(deletion.deletedAtEpochMilliseconds >= membership.createdAtEpochMilliseconds) {
                "Group deletion predates the membership attempt"
            }
            val ownerIdentity =
                contactDao.findPublicIdentityByContactId(context.contactId)
                    ?: error("Group owner identity was not found")
            membershipPacketProtocol
                .verifyConversationDeleted(
                    packet = deletion,
                    expectedOwnerSigningPublicKey = ownerIdentity.signingPublicKey
                ).getOrThrow()

            deleteConversationLocalAttachments(deletion.groupId).getOrThrow()
            groupSecurityManager.deleteLocalGroup(deletion.groupId).getOrThrow()
            chatDao.deleteConversationParticipants(deletion.groupId)
            groupVerificationDao.deleteByGroupId(deletion.groupId)
            if (membership.status != GroupMembershipStatus.GROUP_DELETED.name) {
                val updated =
                    groupMembershipDao.updateStatus(
                        membershipId = membership.membershipId,
                        expectedStatus = membership.status,
                        newStatus =
                            GroupMembershipStateMachine.transition(
                                membership.status,
                                GroupMembershipEvent.GROUP_DELETED
                            ).name,
                        updatedAt = deletion.deletedAtEpochMilliseconds
                    )
                check(updated == 1) { "Group membership changed while deletion was applied" }
            }
            chatDao.updateConversationTimestamp(
                conversationId = deletion.groupId,
                timestamp = deletion.deletedAtEpochMilliseconds
            )
        }
}
