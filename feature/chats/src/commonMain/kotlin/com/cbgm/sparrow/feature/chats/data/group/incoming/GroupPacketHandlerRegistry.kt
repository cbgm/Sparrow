package com.cbgm.sparrow.feature.chats.data.group.incoming

import com.cbgm.sparrow.core.protocol.packet.SparrowPacket
import com.cbgm.sparrow.feature.chats.data.group.incoming.handler.GroupAvatarUpdatedPacketHandler
import com.cbgm.sparrow.feature.chats.data.group.incoming.handler.GroupChatMessagePacketHandler
import com.cbgm.sparrow.feature.chats.data.group.incoming.handler.GroupDescriptionUpdatedPacketHandler
import com.cbgm.sparrow.feature.chats.data.group.incoming.handler.GroupMessageDeletionPacketHandler
import com.cbgm.sparrow.feature.chats.data.group.incoming.handler.GroupMessageEditPacketHandler
import com.cbgm.sparrow.feature.chats.data.group.incoming.handler.GroupPacketHandler
import com.cbgm.sparrow.feature.chats.data.group.incoming.handler.GroupPinUpdatedPacketHandler
import com.cbgm.sparrow.feature.chats.data.group.incoming.handler.GroupTitleUpdatedPacketHandler
import com.cbgm.sparrow.feature.chats.runtime.group.incoming.GroupVerificationReceiptPacketHandler
import com.cbgm.sparrow.feature.chats.runtime.group.incoming.GroupVerificationSnapshotPacketHandler
import com.cbgm.sparrow.feature.chats.runtime.group.incoming.GroupVerificationSnapshotRequestPacketHandler

class GroupPacketHandlerRegistry internal constructor(
    avatarUpdated: GroupAvatarUpdatedPacketHandler,
    descriptionUpdated: GroupDescriptionUpdatedPacketHandler,
    titleUpdated: GroupTitleUpdatedPacketHandler,
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
            descriptionUpdated,
            titleUpdated,
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
