package com.cbgm.sparrow.feature.chats.data.group.incoming

import com.cbgm.sparrow.core.protocol.packet.GroupConversationDeletedPacket
import com.cbgm.sparrow.core.protocol.packet.GroupInvitePacket
import com.cbgm.sparrow.core.protocol.packet.SparrowPacket
import com.cbgm.sparrow.feature.chats.data.group.datasource.GroupIncomingConversationDataSource
import com.cbgm.sparrow.feature.chats.data.group.mapper.GroupMembershipMessageFactory
import com.cbgm.sparrow.feature.membership.domain.usecase.WasGroupMembershipDeletedUseCase

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
