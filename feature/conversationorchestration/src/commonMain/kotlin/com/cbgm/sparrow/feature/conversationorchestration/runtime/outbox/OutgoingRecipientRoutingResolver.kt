package com.cbgm.sparrow.feature.conversationorchestration.runtime.outbox

import com.cbgm.sparrow.core.protocol.packet.ContactInviteAcceptedPacket
import com.cbgm.sparrow.core.protocol.packet.ContactInviteDeclinedPacket
import com.cbgm.sparrow.core.protocol.packet.ContactInvitePacket
import com.cbgm.sparrow.core.protocol.packet.DeliveryReceiptPacket
import com.cbgm.sparrow.core.protocol.packet.GroupAvatarUpdatedPacket
import com.cbgm.sparrow.core.protocol.packet.GroupChatMessagePacket
import com.cbgm.sparrow.core.protocol.packet.GroupConversationDeletedPacket
import com.cbgm.sparrow.core.protocol.packet.GroupCreatedPacket
import com.cbgm.sparrow.core.protocol.packet.GroupDescriptionUpdatedPacket
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
import com.cbgm.sparrow.core.protocol.packet.GroupPinUpdatedPacket
import com.cbgm.sparrow.core.protocol.packet.GroupReadyAcknowledgementPacket
import com.cbgm.sparrow.core.protocol.packet.GroupTitleUpdatedPacket
import com.cbgm.sparrow.core.protocol.packet.GroupVerificationReceiptPacket
import com.cbgm.sparrow.core.protocol.packet.GroupVerificationSnapshotPacket
import com.cbgm.sparrow.core.protocol.packet.GroupVerificationSnapshotRequestPacket
import com.cbgm.sparrow.core.protocol.packet.ReadReceiptPacket
import com.cbgm.sparrow.core.protocol.packet.SparrowPacket
import com.cbgm.sparrow.feature.contacts.domain.usecase.ResolveContactBootstrapRoutingIdUseCase
import com.cbgm.sparrow.feature.contacts.domain.usecase.ResolveContactInvitationRoutingIdUseCase
import com.cbgm.sparrow.feature.contacts.domain.usecase.ResolveContactTransportRoutingIdUseCase
import com.cbgm.sparrow.feature.conversationorchestration.runtime.routing.GroupRoutingResolver

class OutgoingRecipientRoutingResolver(
    private val resolveContactRoutingId: ResolveContactTransportRoutingIdUseCase,
    private val resolveContactBootstrapRoutingId: ResolveContactBootstrapRoutingIdUseCase,
    private val resolveContactInvitationRoutingId: ResolveContactInvitationRoutingIdUseCase,
    private val groupRoutingResolver: GroupRoutingResolver
) {
    suspend fun resolve(
        contactId: String,
        packet: SparrowPacket
    ): String =
        when (packet) {
            is ContactInvitePacket,
            is ContactInviteAcceptedPacket,
            is ContactInviteDeclinedPacket ->
                resolveContactInvitationRoutingId(contactId)

            is GroupInvitePacket,
            is GroupInviteReceivedPacket,
            is GroupJoinRequestPacket,
            is GroupInviteDeclinedPacket ->
                resolveContactBootstrapRoutingId(contactId)

            is GroupConversationDeletedPacket ->
                if (packet.epoch == GroupConversationDeletedPacket.PENDING_GROUP_EPOCH) {
                    resolveContactBootstrapRoutingId(contactId)
                } else {
                    groupRoutingResolver.resolve(packet.groupId, contactId)
                }

            is GroupMemberRemovedPacket ->
                if (packet.epoch == GroupMemberRemovedPacket.PENDING_INVITATION_EPOCH) {
                    resolveContactBootstrapRoutingId(contactId)
                } else {
                    groupRoutingResolver
                        .resolveRemovedMember(packet.removedMemberSigningPublicKey)
                }

            is DeliveryReceiptPacket -> resolveReceipt(packet.messageId, contactId)
            is ReadReceiptPacket -> resolveReceipt(packet.messageId, contactId)

            else ->
                packet.groupIdForRouting()
                    ?.let { groupId -> groupRoutingResolver.resolve(groupId, contactId) }
                    ?: resolveContactRoutingId(contactId)
        }

    private suspend fun resolveReceipt(
        messageId: String,
        contactId: String
    ): String =
        groupRoutingResolver
            .resolveForMessage(messageId, contactId)
            ?: resolveContactRoutingId(contactId)

    private fun SparrowPacket.groupIdForRouting(): String? =
        when (this) {
            is GroupAvatarUpdatedPacket -> groupId
            is GroupDescriptionUpdatedPacket -> groupId
            is GroupTitleUpdatedPacket -> groupId
            is GroupChatMessagePacket -> groupId
            is GroupMessageDeletionPacket -> groupId
            is GroupMessageEditPacket -> groupId
            is GroupPinUpdatedPacket -> groupId
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
