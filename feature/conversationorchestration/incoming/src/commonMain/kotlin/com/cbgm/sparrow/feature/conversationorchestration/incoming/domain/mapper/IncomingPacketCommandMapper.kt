package com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.mapper

import com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.command.IncomingCommand
import com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.model.ReceivedPacket
import com.cbgm.sparrow.protocol.packet.ContactInviteAcceptedPacket
import com.cbgm.sparrow.protocol.packet.ContactInviteDeclinedPacket
import com.cbgm.sparrow.protocol.packet.ContactInvitePacket
import com.cbgm.sparrow.protocol.packet.ContactReadyPacket
import com.cbgm.sparrow.protocol.packet.DirectChatAuthorizationRevokedPacket
import com.cbgm.sparrow.protocol.packet.GroupAvatarUpdatedPacket
import com.cbgm.sparrow.protocol.packet.GroupChatMessagePacket
import com.cbgm.sparrow.protocol.packet.GroupConversationDeletedPacket
import com.cbgm.sparrow.protocol.packet.GroupCreatedPacket
import com.cbgm.sparrow.protocol.packet.GroupDescriptionUpdatedPacket
import com.cbgm.sparrow.protocol.packet.GroupInviteDeclinedPacket
import com.cbgm.sparrow.protocol.packet.GroupInvitePacket
import com.cbgm.sparrow.protocol.packet.GroupInviteReceivedPacket
import com.cbgm.sparrow.protocol.packet.GroupJoinRequestPacket
import com.cbgm.sparrow.protocol.packet.GroupLeaveRequestPacket
import com.cbgm.sparrow.protocol.packet.GroupMemberActivatedPacket
import com.cbgm.sparrow.protocol.packet.GroupMemberActivationAcknowledgementPacket
import com.cbgm.sparrow.protocol.packet.GroupMemberRemovedPacket
import com.cbgm.sparrow.protocol.packet.GroupPinUpdatedPacket
import com.cbgm.sparrow.protocol.packet.GroupReadyAcknowledgementPacket
import com.cbgm.sparrow.protocol.packet.GroupTitleUpdatedPacket
import com.cbgm.sparrow.protocol.packet.GroupVerificationReceiptPacket
import com.cbgm.sparrow.protocol.packet.GroupVerificationSnapshotPacket
import com.cbgm.sparrow.protocol.packet.GroupVerificationSnapshotRequestPacket
import com.cbgm.sparrow.protocol.packet.IdentityAcknowledgementPacket
import com.cbgm.sparrow.protocol.packet.IdentityPacket

/** The workflow command for a group conversation packet, or null when no group workflow handles it. */
@Suppress("CyclomaticComplexMethod")
internal fun ReceivedPacket.toGroupCommand(): IncomingCommand? =
    when (val packet = packet) {
        is GroupChatMessagePacket -> IncomingCommand.ReceiveGroupMessage(context, packet)
        is GroupTitleUpdatedPacket -> IncomingCommand.ReceiveGroupTitle(context, packet)
        is GroupDescriptionUpdatedPacket -> IncomingCommand.ReceiveGroupDescription(context, packet)
        is GroupAvatarUpdatedPacket -> IncomingCommand.ReceiveGroupAvatar(context, packet)
        is GroupPinUpdatedPacket -> IncomingCommand.ReceiveGroupPin(context, packet)
        is GroupVerificationReceiptPacket -> IncomingCommand.ReceiveVerificationReceipt(context, packet)
        is GroupVerificationSnapshotRequestPacket -> IncomingCommand.ReceiveVerificationSnapshotRequest(context, packet)
        is GroupVerificationSnapshotPacket -> IncomingCommand.ReceiveVerificationSnapshot(context, packet)
        is GroupCreatedPacket -> IncomingCommand.ReceiveGroupWelcome(context, packet)
        is GroupMemberActivatedPacket -> IncomingCommand.ReceiveGroupMemberActivation(context, packet)
        is GroupMemberRemovedPacket -> IncomingCommand.ReceiveGroupMemberRemoval(context, packet)
        is GroupConversationDeletedPacket -> IncomingCommand.ReceiveGroupDeletion(context, packet)
        is GroupReadyAcknowledgementPacket -> IncomingCommand.ReceiveGroupReadyAcknowledgement(context, packet)
        is GroupMemberActivationAcknowledgementPacket ->
            IncomingCommand.ReceiveGroupActivationAcknowledgement(context, packet)
        is GroupLeaveRequestPacket -> IncomingCommand.ReceiveGroupLeaveRequest(context, packet)
        else -> null
    }

/** The workflow command for an identity or invitation packet, or null when no workflow handles it. */
@Suppress("CyclomaticComplexMethod")
internal fun ReceivedPacket.toControlCommand(): IncomingCommand? =
    when (val packet = packet) {
        is ContactInvitePacket -> IncomingCommand.ReceiveContactInvite(context, packet)
        is ContactInviteAcceptedPacket -> IncomingCommand.ReceiveContactInviteAccepted(context, packet)
        is ContactInviteDeclinedPacket -> IncomingCommand.ReceiveContactInviteDeclined(context, packet)
        is ContactReadyPacket -> IncomingCommand.ReceiveContactReady(context, packet)
        is DirectChatAuthorizationRevokedPacket -> IncomingCommand.ReceiveAuthorizationRevoked(context, packet)
        is IdentityPacket -> IncomingCommand.ReceiveManualIdentity(context, packet)
        is IdentityAcknowledgementPacket -> IncomingCommand.ReceiveIdentityAcknowledgement(context, packet)
        is GroupInvitePacket -> IncomingCommand.ReceiveGroupInvite(context, packet)
        is GroupInviteReceivedPacket -> IncomingCommand.ReceiveGroupInviteReceipt(context, packet)
        is GroupInviteDeclinedPacket -> IncomingCommand.ReceiveGroupInviteDecline(context, packet)
        is GroupJoinRequestPacket -> IncomingCommand.ReceiveGroupJoinRequest(context, packet)
        else -> null
    }
