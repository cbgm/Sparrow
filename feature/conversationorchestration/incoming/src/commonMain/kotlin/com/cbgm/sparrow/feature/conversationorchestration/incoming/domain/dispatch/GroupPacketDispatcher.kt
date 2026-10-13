package com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.dispatch

import com.cbgm.sparrow.feature.conversationorchestration.engine.runner.WorkflowEngine
import com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.mapper.toGroupCommand
import com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.model.ReceivedPacket
import com.cbgm.sparrow.protocol.packet.GroupAvatarUpdatedPacket
import com.cbgm.sparrow.protocol.packet.GroupChatMessagePacket
import com.cbgm.sparrow.protocol.packet.GroupConversationDeletedPacket
import com.cbgm.sparrow.protocol.packet.GroupCreatedPacket
import com.cbgm.sparrow.protocol.packet.GroupDescriptionUpdatedPacket
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
import com.cbgm.sparrow.protocol.packet.SparrowPacket

/** Starts the one workflow that handles a packet of a group conversation. */
class GroupPacketDispatcher internal constructor(
    private val engine: WorkflowEngine
) {
    suspend fun dispatch(received: ReceivedPacket): Result<Unit> {
        val command = received.toGroupCommand()
            ?: return Result.failure(
                IllegalArgumentException("No group workflow handles ${received.packet::class.simpleName}")
            )
        return engine.run(command).map { }
    }
}

/** The group a packet belongs to, or null for direct and control packets. */
fun SparrowPacket.groupIdOrNull(): String? =
    when (this) {
        is GroupAvatarUpdatedPacket -> groupId
        is GroupCreatedPacket -> groupId
        is GroupConversationDeletedPacket -> groupId
        is GroupDescriptionUpdatedPacket -> groupId
        is GroupTitleUpdatedPacket -> groupId
        is GroupMemberActivatedPacket -> groupId
        is GroupMemberRemovedPacket -> groupId
        is GroupMemberActivationAcknowledgementPacket -> groupId
        is GroupChatMessagePacket -> groupId
        is GroupPinUpdatedPacket -> groupId
        is GroupLeaveRequestPacket -> groupId
        is GroupReadyAcknowledgementPacket -> groupId
        is GroupVerificationReceiptPacket -> groupId
        is GroupVerificationSnapshotRequestPacket -> groupId
        is GroupVerificationSnapshotPacket -> groupId
        else -> null
    }
