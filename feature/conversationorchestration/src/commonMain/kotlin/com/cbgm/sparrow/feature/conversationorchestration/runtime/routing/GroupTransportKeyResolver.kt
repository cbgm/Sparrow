package com.cbgm.sparrow.feature.conversationorchestration.runtime.routing

import com.cbgm.sparrow.feature.conversationorchestration.domain.port.ConversationPort
import com.cbgm.sparrow.feature.membership.domain.usecase.ResolveGroupTransportEncryptionPublicKeyUseCase
import com.cbgm.sparrow.protocol.packet.DeliveryReceiptPacket
import com.cbgm.sparrow.protocol.packet.GroupAvatarUpdatedPacket
import com.cbgm.sparrow.protocol.packet.GroupChatMessagePacket
import com.cbgm.sparrow.protocol.packet.GroupConversationDeletedPacket
import com.cbgm.sparrow.protocol.packet.GroupCreatedPacket
import com.cbgm.sparrow.protocol.packet.GroupDescriptionUpdatedPacket
import com.cbgm.sparrow.protocol.packet.GroupInviteDeclinedPacket
import com.cbgm.sparrow.protocol.packet.GroupInvitePacket
import com.cbgm.sparrow.protocol.packet.GroupJoinRequestPacket
import com.cbgm.sparrow.protocol.packet.GroupLeaveRequestPacket
import com.cbgm.sparrow.protocol.packet.GroupMemberActivatedPacket
import com.cbgm.sparrow.protocol.packet.GroupMemberActivationAcknowledgementPacket
import com.cbgm.sparrow.protocol.packet.GroupMemberRemovedPacket
import com.cbgm.sparrow.protocol.packet.GroupMessageDeletionPacket
import com.cbgm.sparrow.protocol.packet.GroupMessageEditPacket
import com.cbgm.sparrow.protocol.packet.GroupPinUpdatedPacket
import com.cbgm.sparrow.protocol.packet.GroupReadyAcknowledgementPacket
import com.cbgm.sparrow.protocol.packet.GroupTitleUpdatedPacket
import com.cbgm.sparrow.protocol.packet.GroupVerificationReceiptPacket
import com.cbgm.sparrow.protocol.packet.GroupVerificationSnapshotPacket
import com.cbgm.sparrow.protocol.packet.GroupVerificationSnapshotRequestPacket
import com.cbgm.sparrow.protocol.packet.ReadReceiptPacket
import com.cbgm.sparrow.protocol.packet.SparrowPacket

class GroupTransportKeyResolver(
    private val conversationPort: ConversationPort,
    private val resolveMemberEncryptionKey: ResolveGroupTransportEncryptionPublicKeyUseCase
) {
    suspend fun resolveEncryptionPublicKey(
        packet: SparrowPacket,
        contactId: String
    ): ByteArray? {
        require(contactId.isNotBlank()) { "Contact ID must not be blank" }
        val groupId = resolveGroupId(packet) ?: return null
        return resolveMemberEncryptionKey(
            groupId = groupId,
            contactId = contactId,
            useLatestMemberKey = packet is GroupMemberRemovedPacket &&
                packet.epoch > GroupMemberRemovedPacket.PENDING_INVITATION_EPOCH
        ).getOrThrow()
    }

    private suspend fun resolveGroupId(packet: SparrowPacket): String? =
        packet.groupIdOrNull()
            ?: packet.receiptMessageIdOrNull()?.let { messageId ->
                conversationPort.findGroupIdForMessage(messageId).getOrThrow()
            }

    private fun SparrowPacket.receiptMessageIdOrNull(): String? =
        when (this) {
            is DeliveryReceiptPacket -> messageId
            is ReadReceiptPacket -> messageId
            else -> null
        }

    private fun SparrowPacket.groupIdOrNull(): String? =
        when (this) {
            is GroupAvatarUpdatedPacket -> groupId
            is GroupDescriptionUpdatedPacket -> groupId
            is GroupTitleUpdatedPacket -> groupId
            is GroupChatMessagePacket -> groupId
            is GroupMessageDeletionPacket -> groupId
            is GroupMessageEditPacket -> groupId
            is GroupPinUpdatedPacket -> groupId
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
}
