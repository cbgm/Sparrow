package com.cbgm.securechat.feature.chats.data.group.incoming

import com.cbgm.securechat.core.protocol.handler.IncomingPacketContext
import com.cbgm.securechat.core.protocol.packet.GroupChatMessagePacket
import com.cbgm.securechat.core.protocol.packet.GroupConversationDeletedPacket
import com.cbgm.securechat.core.protocol.packet.GroupCreatedPacket
import com.cbgm.securechat.core.protocol.packet.GroupInviteDeclinedPacket
import com.cbgm.securechat.core.protocol.packet.GroupInvitePacket
import com.cbgm.securechat.core.protocol.packet.GroupInviteReceivedPacket
import com.cbgm.securechat.core.protocol.packet.GroupJoinRequestPacket
import com.cbgm.securechat.core.protocol.packet.GroupLeaveRequestPacket
import com.cbgm.securechat.core.protocol.packet.GroupMemberActivatedPacket
import com.cbgm.securechat.core.protocol.packet.GroupMemberActivationAcknowledgementPacket
import com.cbgm.securechat.core.protocol.packet.GroupMemberRemovedPacket
import com.cbgm.securechat.core.protocol.packet.GroupReadyAcknowledgementPacket
import com.cbgm.securechat.core.protocol.packet.GroupVerificationReceiptPacket
import com.cbgm.securechat.core.protocol.packet.GroupVerificationSnapshotPacket
import com.cbgm.securechat.core.protocol.packet.GroupVerificationSnapshotRequestPacket
import com.cbgm.securechat.core.protocol.packet.SecureChatPacket
import com.cbgm.securechat.feature.chats.data.model.DecodedIncomingPacket

class GroupIncomingPacketProcessor(
    private val policy: GroupIncomingPacketPolicy,
    private val handlerRegistry: GroupPacketHandlerRegistry
) {
    fun canProcess(packet: SecureChatPacket): Boolean = packet.groupIdOrNull() != null

    suspend fun process(incoming: DecodedIncomingPacket): Result<Unit> =
        runCatching {
            val groupId = requireNotNull(incoming.packet.groupIdOrNull()) { "Packet is not a group packet" }
            if (policy.shouldIgnore(groupId, incoming.packet)) return@runCatching
            val handler = handlerRegistry.find(incoming.packet)
                ?: error("No group packet handler registered for ${incoming.packet::class.simpleName}")
            handler.handle(incoming.toContext(groupId), incoming.packet).getOrThrow()
        }

    private fun DecodedIncomingPacket.toContext(groupId: String): IncomingPacketContext =
        IncomingPacketContext(
            contactId = contactId,
            conversationId = groupId,
            encodedTransportPayload = encodedTransportPayload,
            transportMode = transportMode,
            receivedAtEpochMilliseconds = receivedAtEpochMilliseconds
        )
}

internal fun SecureChatPacket.groupIdOrNull(): String? =
    when (this) {
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
