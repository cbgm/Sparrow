package com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.command

import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.WorkflowCommand
import com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.model.OperationConversation
import com.cbgm.sparrow.protocol.handler.IncomingPacketContext
import com.cbgm.sparrow.protocol.message.MessageOperation
import com.cbgm.sparrow.protocol.packet.ChatMessagePacket
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
import com.cbgm.sparrow.protocol.packet.SparrowPacket

/**
 * Every incoming packet job orchestration starts internally, from a runtime observer or another workflow.
 * Submit one with `engine.run(command)`; the engine finds the workflow that handles it.
 */
sealed interface IncomingCommand : WorkflowCommand {
    /** A decoded packet from [contactId] whose conversation is not known yet. */
    data class RouteIncomingPacket(
        val contactId: String,
        val packet: SparrowPacket,
        val encodedTransportPayload: String,
        val transportMode: String,
        val receivedAtEpochMilliseconds: Long
    ) : IncomingCommand

    /** A contact edited, deleted, reacted to or voted on a message in [conversationId]. */
    data class ApplyRemoteMessageOperation(
        val conversationId: String,
        val senderContactId: String,
        val operation: MessageOperation,
        val conversation: OperationConversation,
        val sentAtEpochMilliseconds: Long
    ) : IncomingCommand

    /** A manual identity exchange with the contact just became mutual. */
    data class CompleteManualIdentity(
        val contactId: String
    ) : IncomingCommand

    /** Applies a received [DirectChatAuthorizationRevokedPacket]; started by the packet dispatchers. */
    data class ReceiveAuthorizationRevoked(
        val context: IncomingPacketContext,
        val packet: DirectChatAuthorizationRevokedPacket
    ) : IncomingCommand

    /** Applies a received [ContactInvitePacket]; started by the packet dispatchers. */
    data class ReceiveContactInvite(
        val context: IncomingPacketContext,
        val packet: ContactInvitePacket
    ) : IncomingCommand

    /** Applies a received [ContactInviteAcceptedPacket]; started by the packet dispatchers. */
    data class ReceiveContactInviteAccepted(
        val context: IncomingPacketContext,
        val packet: ContactInviteAcceptedPacket
    ) : IncomingCommand

    /** Applies a received [ContactInviteDeclinedPacket]; started by the packet dispatchers. */
    data class ReceiveContactInviteDeclined(
        val context: IncomingPacketContext,
        val packet: ContactInviteDeclinedPacket
    ) : IncomingCommand

    /** Applies a received [ContactReadyPacket]; started by the packet dispatchers. */
    data class ReceiveContactReady(
        val context: IncomingPacketContext,
        val packet: ContactReadyPacket
    ) : IncomingCommand

    /** Applies a received [ChatMessagePacket]; started by the packet dispatchers. */
    data class ReceiveDirectMessage(
        val context: IncomingPacketContext,
        val packet: ChatMessagePacket
    ) : IncomingCommand

    /** Applies a received [GroupMemberActivationAcknowledgementPacket]; started by the packet dispatchers. */
    data class ReceiveGroupActivationAcknowledgement(
        val context: IncomingPacketContext,
        val packet: GroupMemberActivationAcknowledgementPacket
    ) : IncomingCommand

    /** Applies a received [GroupAvatarUpdatedPacket]; started by the packet dispatchers. */
    data class ReceiveGroupAvatar(
        val context: IncomingPacketContext,
        val packet: GroupAvatarUpdatedPacket
    ) : IncomingCommand

    /** Applies a received [GroupConversationDeletedPacket]; started by the packet dispatchers. */
    data class ReceiveGroupDeletion(
        val context: IncomingPacketContext,
        val packet: GroupConversationDeletedPacket
    ) : IncomingCommand

    /** Applies a received [GroupDescriptionUpdatedPacket]; started by the packet dispatchers. */
    data class ReceiveGroupDescription(
        val context: IncomingPacketContext,
        val packet: GroupDescriptionUpdatedPacket
    ) : IncomingCommand

    /** Applies a received [GroupInvitePacket]; started by the packet dispatchers. */
    data class ReceiveGroupInvite(
        val context: IncomingPacketContext,
        val packet: GroupInvitePacket
    ) : IncomingCommand

    /** Applies a received [GroupInviteDeclinedPacket]; started by the packet dispatchers. */
    data class ReceiveGroupInviteDecline(
        val context: IncomingPacketContext,
        val packet: GroupInviteDeclinedPacket
    ) : IncomingCommand

    /** Applies a received [GroupInviteReceivedPacket]; started by the packet dispatchers. */
    data class ReceiveGroupInviteReceipt(
        val context: IncomingPacketContext,
        val packet: GroupInviteReceivedPacket
    ) : IncomingCommand

    /** Applies a received [GroupJoinRequestPacket]; started by the packet dispatchers. */
    data class ReceiveGroupJoinRequest(
        val context: IncomingPacketContext,
        val packet: GroupJoinRequestPacket
    ) : IncomingCommand

    /** Applies a received [GroupLeaveRequestPacket]; started by the packet dispatchers. */
    data class ReceiveGroupLeaveRequest(
        val context: IncomingPacketContext,
        val packet: GroupLeaveRequestPacket
    ) : IncomingCommand

    /** Applies a received [GroupMemberActivatedPacket]; started by the packet dispatchers. */
    data class ReceiveGroupMemberActivation(
        val context: IncomingPacketContext,
        val packet: GroupMemberActivatedPacket
    ) : IncomingCommand

    /** Applies a received [GroupMemberRemovedPacket]; started by the packet dispatchers. */
    data class ReceiveGroupMemberRemoval(
        val context: IncomingPacketContext,
        val packet: GroupMemberRemovedPacket
    ) : IncomingCommand

    /** Applies a received [GroupChatMessagePacket]; started by the packet dispatchers. */
    data class ReceiveGroupMessage(
        val context: IncomingPacketContext,
        val packet: GroupChatMessagePacket
    ) : IncomingCommand

    /** Applies a received [GroupPinUpdatedPacket]; started by the packet dispatchers. */
    data class ReceiveGroupPin(
        val context: IncomingPacketContext,
        val packet: GroupPinUpdatedPacket
    ) : IncomingCommand

    /** Applies a received [GroupReadyAcknowledgementPacket]; started by the packet dispatchers. */
    data class ReceiveGroupReadyAcknowledgement(
        val context: IncomingPacketContext,
        val packet: GroupReadyAcknowledgementPacket
    ) : IncomingCommand

    /** Applies a received [GroupTitleUpdatedPacket]; started by the packet dispatchers. */
    data class ReceiveGroupTitle(
        val context: IncomingPacketContext,
        val packet: GroupTitleUpdatedPacket
    ) : IncomingCommand

    /** Applies a received [GroupCreatedPacket]; started by the packet dispatchers. */
    data class ReceiveGroupWelcome(
        val context: IncomingPacketContext,
        val packet: GroupCreatedPacket
    ) : IncomingCommand

    /** Applies a received [IdentityAcknowledgementPacket]; started by the packet dispatchers. */
    data class ReceiveIdentityAcknowledgement(
        val context: IncomingPacketContext,
        val packet: IdentityAcknowledgementPacket
    ) : IncomingCommand

    /** Applies a received [IdentityPacket]; started by the packet dispatchers. */
    data class ReceiveManualIdentity(
        val context: IncomingPacketContext,
        val packet: IdentityPacket
    ) : IncomingCommand

    /** Applies a received [GroupVerificationReceiptPacket]; started by the packet dispatchers. */
    data class ReceiveVerificationReceipt(
        val context: IncomingPacketContext,
        val packet: GroupVerificationReceiptPacket
    ) : IncomingCommand

    /** Applies a received [GroupVerificationSnapshotPacket]; started by the packet dispatchers. */
    data class ReceiveVerificationSnapshot(
        val context: IncomingPacketContext,
        val packet: GroupVerificationSnapshotPacket
    ) : IncomingCommand

    /** Applies a received [GroupVerificationSnapshotRequestPacket]; started by the packet dispatchers. */
    data class ReceiveVerificationSnapshotRequest(
        val context: IncomingPacketContext,
        val packet: GroupVerificationSnapshotRequestPacket
    ) : IncomingCommand
}
