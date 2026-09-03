package com.cbgm.sparrow.feature.messaging.runtime.outbox

import com.cbgm.sparrow.core.protocol.packet.ContactInviteAcceptedPacket
import com.cbgm.sparrow.core.protocol.packet.ContactInviteDeclinedPacket
import com.cbgm.sparrow.core.protocol.packet.ContactInvitePacket
import com.cbgm.sparrow.core.protocol.packet.DeliveryReceiptPacket
import com.cbgm.sparrow.core.protocol.packet.GroupAvatarUpdatedPacket
import com.cbgm.sparrow.core.protocol.packet.GroupChatMessagePacket
import com.cbgm.sparrow.core.protocol.packet.GroupConversationDeletedPacket
import com.cbgm.sparrow.core.protocol.packet.GroupCreatedPacket
import com.cbgm.sparrow.core.protocol.packet.GroupInviteDeclinedPacket
import com.cbgm.sparrow.core.protocol.packet.GroupInvitePacket
import com.cbgm.sparrow.core.protocol.packet.GroupInviteReceivedPacket
import com.cbgm.sparrow.core.protocol.packet.GroupJoinRequestPacket
import com.cbgm.sparrow.core.protocol.packet.GroupLeaveRequestPacket
import com.cbgm.sparrow.core.protocol.packet.GroupMemberActivatedPacket
import com.cbgm.sparrow.core.protocol.packet.GroupMemberActivationAcknowledgementPacket
import com.cbgm.sparrow.core.protocol.packet.GroupMemberRemovedPacket
import com.cbgm.sparrow.core.protocol.packet.GroupMessageDeletionPacket
import com.cbgm.sparrow.core.protocol.packet.GroupMessageEditPacket
import com.cbgm.sparrow.core.protocol.packet.GroupReadyAcknowledgementPacket
import com.cbgm.sparrow.core.protocol.packet.GroupVerificationReceiptPacket
import com.cbgm.sparrow.core.protocol.packet.GroupVerificationSnapshotPacket
import com.cbgm.sparrow.core.protocol.packet.GroupVerificationSnapshotRequestPacket
import com.cbgm.sparrow.core.protocol.packet.ReadReceiptPacket
import com.cbgm.sparrow.core.protocol.packet.SparrowPacket
import com.cbgm.sparrow.feature.messaging.data.datasource.ContactRoutingDataSource
import com.cbgm.sparrow.feature.messaging.data.datasource.GroupRoutingDataSource

class OutgoingRecipientRoutingResolver(
    private val contactRoutingDataSource: ContactRoutingDataSource,
    private val groupRoutingDataSource: GroupRoutingDataSource
) {
    suspend fun resolve(
        contactId: String,
        packet: SparrowPacket
    ): String =
        when (packet) {
            is ContactInvitePacket,
            is ContactInviteAcceptedPacket,
            is ContactInviteDeclinedPacket,
            is GroupInvitePacket,
            is GroupInviteReceivedPacket,
            is GroupJoinRequestPacket,
            is GroupInviteDeclinedPacket ->
                contactRoutingDataSource.resolveBootstrap(contactId).getOrThrow()

            is GroupConversationDeletedPacket ->
                if (packet.epoch == GroupConversationDeletedPacket.PENDING_GROUP_EPOCH) {
                    contactRoutingDataSource.resolveBootstrap(contactId).getOrThrow()
                } else {
                    groupRoutingDataSource.resolve(packet.groupId, contactId).getOrThrow()
                }

            is GroupMemberRemovedPacket ->
                if (packet.epoch == GroupMemberRemovedPacket.PENDING_INVITATION_EPOCH) {
                    contactRoutingDataSource.resolveBootstrap(contactId).getOrThrow()
                } else {
                    groupRoutingDataSource
                        .resolveRemovedMember(packet.removedMemberSigningPublicKey)
                        .getOrThrow()
                }

            is DeliveryReceiptPacket -> resolveReceipt(packet.messageId, contactId)
            is ReadReceiptPacket -> resolveReceipt(packet.messageId, contactId)

            else ->
                packet.groupIdForRouting()
                    ?.let { groupId -> groupRoutingDataSource.resolve(groupId, contactId).getOrThrow() }
                    ?: contactRoutingDataSource.resolve(contactId).getOrThrow()
        }

    private suspend fun resolveReceipt(
        messageId: String,
        contactId: String
    ): String =
        groupRoutingDataSource
            .resolveForMessage(messageId, contactId)
            .getOrThrow()
            ?: contactRoutingDataSource.resolve(contactId).getOrThrow()

    private fun SparrowPacket.groupIdForRouting(): String? =
        when (this) {
            is GroupAvatarUpdatedPacket -> groupId
            is GroupChatMessagePacket -> groupId
            is GroupMessageDeletionPacket -> groupId
            is GroupMessageEditPacket -> groupId
            is GroupCreatedPacket -> groupId
            is GroupLeaveRequestPacket -> groupId
            is GroupMemberActivatedPacket -> groupId
            is GroupMemberActivationAcknowledgementPacket -> groupId
            is GroupReadyAcknowledgementPacket -> groupId
            is GroupVerificationReceiptPacket -> groupId
            is GroupVerificationSnapshotRequestPacket -> groupId
            is GroupVerificationSnapshotPacket -> groupId
            else -> null
        }
}
