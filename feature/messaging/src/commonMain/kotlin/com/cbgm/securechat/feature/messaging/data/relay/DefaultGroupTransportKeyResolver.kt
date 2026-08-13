package com.cbgm.securechat.feature.messaging.data.relay

import com.cbgm.securechat.core.protocol.packet.DeliveryReceiptPacket
import com.cbgm.securechat.core.protocol.packet.GroupChatMessagePacket
import com.cbgm.securechat.core.protocol.packet.GroupConversationDeletedPacket
import com.cbgm.securechat.core.protocol.packet.GroupCreatedPacket
import com.cbgm.securechat.core.protocol.packet.GroupInviteDeclinedPacket
import com.cbgm.securechat.core.protocol.packet.GroupInvitePacket
import com.cbgm.securechat.core.protocol.packet.GroupJoinRequestPacket
import com.cbgm.securechat.core.protocol.packet.GroupLeaveRequestPacket
import com.cbgm.securechat.core.protocol.packet.GroupMemberActivatedPacket
import com.cbgm.securechat.core.protocol.packet.GroupMemberActivationAcknowledgementPacket
import com.cbgm.securechat.core.protocol.packet.GroupMemberRemovedPacket
import com.cbgm.securechat.core.protocol.packet.GroupReadyAcknowledgementPacket
import com.cbgm.securechat.core.protocol.packet.GroupVerificationReceiptPacket
import com.cbgm.securechat.core.protocol.packet.GroupVerificationSnapshotPacket
import com.cbgm.securechat.core.protocol.packet.GroupVerificationSnapshotRequestPacket
import com.cbgm.securechat.core.protocol.packet.ReadReceiptPacket
import com.cbgm.securechat.core.protocol.packet.SecureChatPacket
import com.cbgm.securechat.data.database.dao.ChatDao
import com.cbgm.securechat.data.database.dao.GroupSecurityDao
import com.cbgm.securechat.feature.messaging.domain.relay.GroupTransportKeyResolver

class DefaultGroupTransportKeyResolver(
    private val chatDao: ChatDao,
    private val groupSecurityDao: GroupSecurityDao
) : GroupTransportKeyResolver {
    override suspend fun resolveEncryptionPublicKey(
        packet: SecureChatPacket,
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

    private suspend fun resolveGroupId(packet: SecureChatPacket): String? =
        packet.groupIdOrNull()
            ?: packet.receiptMessageIdOrNull()?.let { messageId ->
                val message = chatDao.findMessageById(messageId) ?: return@let null
                val conversation = chatDao.findConversationById(message.conversationId) ?: return@let null
                conversation.id.takeIf { conversation.type == GROUP_CONVERSATION_TYPE }
            }

    private fun SecureChatPacket.receiptMessageIdOrNull(): String? =
        when (this) {
            is DeliveryReceiptPacket -> messageId
            is ReadReceiptPacket -> messageId
            else -> null
        }

    private fun SecureChatPacket.groupIdOrNull(): String? =
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
