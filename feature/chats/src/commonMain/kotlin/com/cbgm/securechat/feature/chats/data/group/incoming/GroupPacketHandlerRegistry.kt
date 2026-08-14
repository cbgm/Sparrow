package com.cbgm.securechat.feature.chats.data.group.incoming

import com.cbgm.securechat.core.protocol.packet.SecureChatPacket
import com.cbgm.securechat.feature.chats.data.group.incoming.handler.GroupChatMessagePacketHandler
import com.cbgm.securechat.feature.chats.data.group.incoming.handler.GroupConversationDeletedPacketHandler
import com.cbgm.securechat.feature.chats.data.group.incoming.handler.GroupCreatedPacketHandler
import com.cbgm.securechat.feature.chats.data.group.incoming.handler.GroupInviteDeclinedPacketHandler
import com.cbgm.securechat.feature.chats.data.group.incoming.handler.GroupInvitePacketHandler
import com.cbgm.securechat.feature.chats.data.group.incoming.handler.GroupInviteReceivedPacketHandler
import com.cbgm.securechat.feature.chats.data.group.incoming.handler.GroupJoinRequestPacketHandler
import com.cbgm.securechat.feature.chats.data.group.incoming.handler.GroupLeaveRequestPacketHandler
import com.cbgm.securechat.feature.chats.data.group.incoming.handler.GroupMemberActivatedPacketHandler
import com.cbgm.securechat.feature.chats.data.group.incoming.handler.GroupMemberActivationAcknowledgementPacketHandler
import com.cbgm.securechat.feature.chats.data.group.incoming.handler.GroupMemberRemovedPacketHandler
import com.cbgm.securechat.feature.chats.data.group.incoming.handler.GroupPacketHandler
import com.cbgm.securechat.feature.chats.data.group.incoming.handler.GroupReadyAcknowledgementPacketHandler
import com.cbgm.securechat.feature.chats.data.group.incoming.handler.GroupVerificationReceiptPacketHandler
import com.cbgm.securechat.feature.chats.data.group.incoming.handler.GroupVerificationSnapshotPacketHandler
import com.cbgm.securechat.feature.chats.data.group.incoming.handler.GroupVerificationSnapshotRequestPacketHandler

class GroupPacketHandlerRegistry(
    groupCreated: GroupCreatedPacketHandler,
    conversationDeleted: GroupConversationDeletedPacketHandler,
    invite: GroupInvitePacketHandler,
    inviteReceived: GroupInviteReceivedPacketHandler,
    joinRequest: GroupJoinRequestPacketHandler,
    leaveRequest: GroupLeaveRequestPacketHandler,
    inviteDeclined: GroupInviteDeclinedPacketHandler,
    readyAcknowledgement: GroupReadyAcknowledgementPacketHandler,
    memberActivated: GroupMemberActivatedPacketHandler,
    memberActivationAcknowledgement: GroupMemberActivationAcknowledgementPacketHandler,
    memberRemoved: GroupMemberRemovedPacketHandler,
    verificationReceipt: GroupVerificationReceiptPacketHandler,
    verificationSnapshotRequest: GroupVerificationSnapshotRequestPacketHandler,
    verificationSnapshot: GroupVerificationSnapshotPacketHandler,
    chatMessage: GroupChatMessagePacketHandler
) {
    private val handlers: List<GroupPacketHandler> =
        listOf(
            groupCreated,
            conversationDeleted,
            invite,
            inviteReceived,
            joinRequest,
            leaveRequest,
            inviteDeclined,
            readyAcknowledgement,
            memberActivated,
            memberActivationAcknowledgement,
            memberRemoved,
            verificationReceipt,
            verificationSnapshotRequest,
            verificationSnapshot,
            chatMessage
        )

    fun find(packet: SecureChatPacket): GroupPacketHandler? =
        handlers.firstOrNull { handler -> handler.canHandle(packet) }
}
