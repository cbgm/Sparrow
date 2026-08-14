package com.cbgm.sparrow.feature.messaging.data.routing

import com.cbgm.sparrow.core.protocol.packet.DeliveryReceiptPacket
import com.cbgm.sparrow.core.protocol.packet.GroupChatMessagePacket
import com.cbgm.sparrow.core.protocol.packet.GroupConversationDeletedPacket
import com.cbgm.sparrow.core.protocol.packet.GroupCreatedPacket
import com.cbgm.sparrow.core.protocol.packet.GroupInviteDeclinedPacket
import com.cbgm.sparrow.core.protocol.packet.GroupInvitePacket
import com.cbgm.sparrow.core.protocol.packet.GroupJoinRequestPacket
import com.cbgm.sparrow.core.protocol.packet.GroupLeaveRequestPacket
import com.cbgm.sparrow.core.protocol.packet.GroupMemberActivatedPacket
import com.cbgm.sparrow.core.protocol.packet.GroupMemberActivationAcknowledgementPacket
import com.cbgm.sparrow.core.protocol.packet.GroupMemberRemovedPacket
import com.cbgm.sparrow.core.protocol.packet.GroupReadyAcknowledgementPacket
import com.cbgm.sparrow.core.protocol.packet.GroupVerificationReceiptPacket
import com.cbgm.sparrow.core.protocol.packet.GroupVerificationSnapshotPacket
import com.cbgm.sparrow.core.protocol.packet.GroupVerificationSnapshotRequestPacket
import com.cbgm.sparrow.core.protocol.packet.ReadReceiptPacket
import com.cbgm.sparrow.core.protocol.packet.SparrowPacket
import com.cbgm.sparrow.data.database.dao.ChatDao
import com.cbgm.sparrow.data.database.dao.GroupSecurityDao
import com.cbgm.sparrow.feature.messaging.application.routing.GroupTransportKeyResolver

class DefaultGroupTransportKeyResolver(
    private val chatDao: ChatDao,
    private val groupSecurityDao: GroupSecurityDao
) : GroupTransportKeyResolver {
    override suspend fun resolveEncryptionPublicKey(
        packet: SparrowPacket,
        contactId: String
    ): Result<ByteArray?> =
        runCatching {
            require(contactId.isNotBlank()) { "Contact ID must not be blank" }
            val groupId = resolveGroupId(packet) ?: return@runCatching null
            if (packet is GroupMemberRemovedPacket && packet.epoch > GroupMemberRemovedPacket.PENDING_INVITATION_EPOCH) {
                return@runCatching groupSecurityDao
                    .findLatestMemberKey(
                        groupId = groupId,
                        contactId = contactId
                    )?.encryptionPublicKey
                    ?.copyOf()
            }

            val state = groupSecurityDao.findState(groupId) ?: return@runCatching null
            groupSecurityDao
                .findMemberKey(
                    groupId = groupId,
                    epoch = state.currentEpoch,
                    contactId = contactId
                )?.encryptionPublicKey
                ?.copyOf()
        }

    private suspend fun resolveGroupId(packet: SparrowPacket): String? =
        packet.groupIdOrNull()
            ?: packet.receiptMessageIdOrNull()?.let { messageId ->
                val message = chatDao.findMessageById(messageId) ?: return@let null
                val conversation = chatDao.findConversationById(message.conversationId) ?: return@let null
                conversation.id.takeIf { conversation.type == GROUP_CONVERSATION_TYPE }
            }

    private fun SparrowPacket.receiptMessageIdOrNull(): String? =
        when (this) {
            is DeliveryReceiptPacket -> messageId
            is ReadReceiptPacket -> messageId
            else -> null
        }

    private fun SparrowPacket.groupIdOrNull(): String? =
        when (this) {
            is GroupChatMessagePacket -> groupId
            is GroupConversationDeletedPacket -> groupId
            is GroupCreatedPacket -> groupId
            is GroupInvitePacket -> groupId
            is GroupJoinRequestPacket -> groupId
            is GroupInviteDeclinedPacket -> groupId
            is GroupLeaveRequestPacket -> groupId
            is GroupMemberActivatedPacket -> groupId
            is GroupMemberActivationAcknowledgementPacket -> groupId
            is GroupMemberRemovedPacket -> groupId
            is GroupReadyAcknowledgementPacket -> groupId
            is GroupVerificationReceiptPacket -> groupId
            is GroupVerificationSnapshotRequestPacket -> groupId
            is GroupVerificationSnapshotPacket -> groupId
            else -> null
        }

    private companion object {
        const val GROUP_CONVERSATION_TYPE = "GROUP"
    }
}
