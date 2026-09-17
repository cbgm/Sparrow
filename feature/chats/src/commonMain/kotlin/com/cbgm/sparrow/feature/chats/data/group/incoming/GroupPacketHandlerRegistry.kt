package com.cbgm.sparrow.feature.chats.data.group.incoming

import com.cbgm.sparrow.core.protocol.packet.SparrowPacket
import com.cbgm.sparrow.feature.chats.data.group.incoming.handler.GroupAvatarUpdatedPacketHandler
import com.cbgm.sparrow.feature.chats.data.group.incoming.handler.GroupChatMessagePacketHandler
import com.cbgm.sparrow.feature.chats.data.group.incoming.handler.GroupConversationDeletedPacketHandler
import com.cbgm.sparrow.feature.chats.data.group.incoming.handler.GroupCreatedPacketHandler
import com.cbgm.sparrow.feature.chats.data.group.incoming.handler.GroupDescriptionUpdatedPacketHandler
import com.cbgm.sparrow.feature.chats.data.group.incoming.handler.GroupLeaveRequestPacketHandler
import com.cbgm.sparrow.feature.chats.data.group.incoming.handler.GroupMemberActivatedPacketHandler
import com.cbgm.sparrow.feature.chats.data.group.incoming.handler.GroupMemberActivationAcknowledgementPacketHandler
import com.cbgm.sparrow.feature.chats.data.group.incoming.handler.GroupMemberRemovedPacketHandler
import com.cbgm.sparrow.feature.chats.data.group.incoming.handler.GroupMessageDeletionPacketHandler
import com.cbgm.sparrow.feature.chats.data.group.incoming.handler.GroupMessageEditPacketHandler
import com.cbgm.sparrow.feature.chats.data.group.incoming.handler.GroupPacketHandler
import com.cbgm.sparrow.feature.chats.data.group.incoming.handler.GroupPinUpdatedPacketHandler
import com.cbgm.sparrow.feature.chats.data.group.incoming.handler.GroupReadyAcknowledgementPacketHandler
import com.cbgm.sparrow.feature.chats.data.group.incoming.handler.GroupTitleUpdatedPacketHandler
import com.cbgm.sparrow.feature.chats.data.group.incoming.handler.GroupVerificationReceiptPacketHandler
import com.cbgm.sparrow.feature.chats.data.group.incoming.handler.GroupVerificationSnapshotPacketHandler
import com.cbgm.sparrow.feature.chats.data.group.incoming.handler.GroupVerificationSnapshotRequestPacketHandler

class GroupPacketHandlerRegistry internal constructor(
    avatarUpdated: GroupAvatarUpdatedPacketHandler,
    groupCreated: GroupCreatedPacketHandler,
    conversationDeleted: GroupConversationDeletedPacketHandler,
    descriptionUpdated: GroupDescriptionUpdatedPacketHandler,
    titleUpdated: GroupTitleUpdatedPacketHandler,
    leaveRequest: GroupLeaveRequestPacketHandler,
    readyAcknowledgement: GroupReadyAcknowledgementPacketHandler,
    memberActivated: GroupMemberActivatedPacketHandler,
    memberActivationAcknowledgement: GroupMemberActivationAcknowledgementPacketHandler,
    memberRemoved: GroupMemberRemovedPacketHandler,
    verificationReceipt: GroupVerificationReceiptPacketHandler,
    verificationSnapshotRequest: GroupVerificationSnapshotRequestPacketHandler,
    verificationSnapshot: GroupVerificationSnapshotPacketHandler,
    chatMessage: GroupChatMessagePacketHandler,
    messageDeletion: GroupMessageDeletionPacketHandler,
    messageEdit: GroupMessageEditPacketHandler,
    pinUpdated: GroupPinUpdatedPacketHandler
) {
    private val handlers: List<GroupPacketHandler> =
        listOf(
            avatarUpdated,
            groupCreated,
            conversationDeleted,
            descriptionUpdated,
            titleUpdated,
            leaveRequest,
            readyAcknowledgement,
            memberActivated,
            memberActivationAcknowledgement,
            memberRemoved,
            verificationReceipt,
            verificationSnapshotRequest,
            verificationSnapshot,
            chatMessage,
            messageDeletion,
            messageEdit,
            pinUpdated
        )

    fun find(packet: SparrowPacket): GroupPacketHandler? =
        handlers.firstOrNull { handler -> handler.canHandle(packet) }
}
