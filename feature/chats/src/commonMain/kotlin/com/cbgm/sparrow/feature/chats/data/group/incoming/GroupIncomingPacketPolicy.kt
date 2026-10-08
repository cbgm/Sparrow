package com.cbgm.sparrow.feature.chats.data.group.incoming

import com.cbgm.sparrow.feature.chats.data.group.datasource.GroupIncomingConversationDataSource
import com.cbgm.sparrow.feature.chats.data.group.mapper.GroupMembershipMessageFactory
import com.cbgm.sparrow.feature.membership.domain.usecase.WasGroupMembershipDeletedUseCase
import com.cbgm.sparrow.protocol.packet.GroupConversationDeletedPacket
import com.cbgm.sparrow.protocol.packet.GroupInvitePacket
import com.cbgm.sparrow.protocol.packet.SparrowPacket

class GroupIncomingPacketPolicy(
    private val incomingConversationDataSource: GroupIncomingConversationDataSource,
    private val wasGroupMembershipDeleted: WasGroupMembershipDeletedUseCase
) {
    suspend fun shouldIgnore(
        groupId: String,
        packet: SparrowPacket
    ): Boolean {
        if (packet is GroupInvitePacket) return false
        if (incomingConversationDataSource.hasMessageWithTransportMode(groupId, GroupMembershipMessageFactory.LOCAL_CONVERSATION_DELETED_TRANSPORT_MODE)) {
            return true
        }
        if (packet is GroupConversationDeletedPacket) return false
        return wasGroupMembershipDeleted(groupId).getOrThrow()
    }
}
