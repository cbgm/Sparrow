package com.cbgm.sparrow.feature.chats.data.group.incoming

import com.cbgm.sparrow.core.protocol.handler.IncomingPacketContext
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
import com.cbgm.sparrow.core.protocol.packet.GroupReadyAcknowledgementPacket
import com.cbgm.sparrow.core.protocol.packet.GroupVerificationReceiptPacket
import com.cbgm.sparrow.core.protocol.packet.GroupVerificationSnapshotPacket
import com.cbgm.sparrow.core.protocol.packet.GroupVerificationSnapshotRequestPacket
import com.cbgm.sparrow.core.protocol.packet.SparrowPacket
import com.cbgm.sparrow.feature.chats.data.model.DecodedIncomingPacketDto

class GroupIncomingPacketProcessor(
    private val policy: GroupIncomingPacketPolicy,
    private val handlerRegistry: GroupPacketHandlerRegistry
) {
    fun canProcess(packet: SparrowPacket): Boolean = packet.groupIdOrNull() != null

    suspend fun process(incoming: DecodedIncomingPacketDto): Result<Unit> =
        runCatching {
            val groupId = requireNotNull(incoming.packet.groupIdOrNull()) { "Packet is not a group packet" }
            if (policy.shouldIgnore(groupId, incoming.packet)) return@runCatching
            val handler = handlerRegistry.find(incoming.packet)
                ?: error("No group packet handler registered for ${incoming.packet::class.simpleName}")
            handler.handle(incoming.toIncomingPacketContext(groupId), incoming.packet).getOrThrow()
        }

    private fun DecodedIncomingPacketDto.toIncomingPacketContext(groupId: String): IncomingPacketContext =
        IncomingPacketContext(
            contactId = contactId,
            conversationId = groupId,
            encodedTransportPayload = encodedTransportPayload,
            transportMode = transportMode,
            receivedAtEpochMilliseconds = receivedAtEpochMilliseconds
        )
}

internal fun SparrowPacket.groupIdOrNull(): String? =
    when (this) {
        is GroupAvatarUpdatedPacket -> groupId
        is GroupCreatedPacket -> groupId
        is GroupConversationDeletedPacket -> groupId
        is GroupMemberActivatedPacket -> groupId
        is GroupMemberRemovedPacket -> groupId
        is GroupMemberActivationAcknowledgementPacket -> groupId
        is GroupChatMessagePacket -> groupId
        is GroupInvitePacket -> groupId
        is GroupInviteReceivedPacket -> groupId
        is GroupJoinRequestPacket -> groupId
        is GroupLeaveRequestPacket -> groupId
        is GroupInviteDeclinedPacket -> groupId
        is GroupReadyAcknowledgementPacket -> groupId
        is GroupVerificationReceiptPacket -> groupId
        is GroupVerificationSnapshotRequestPacket -> groupId
        is GroupVerificationSnapshotPacket -> groupId
        else -> null
    }
