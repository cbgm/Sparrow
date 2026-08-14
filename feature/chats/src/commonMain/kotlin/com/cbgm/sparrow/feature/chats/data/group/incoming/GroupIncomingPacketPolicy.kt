package com.cbgm.sparrow.feature.chats.data.group.incoming

import com.cbgm.sparrow.core.protocol.packet.GroupConversationDeletedPacket
import com.cbgm.sparrow.core.protocol.packet.GroupInvitePacket
import com.cbgm.sparrow.core.protocol.packet.SparrowPacket
import com.cbgm.sparrow.data.database.dao.ChatDao
import com.cbgm.sparrow.data.database.dao.GroupInvitationDao
import com.cbgm.sparrow.feature.chats.data.group.invitation.GroupInvitationStatus
import com.cbgm.sparrow.feature.chats.data.group.mapper.GroupMembershipMessageFactory

class GroupIncomingPacketPolicy(
    private val chatDao: ChatDao,
    private val groupInvitationDao: GroupInvitationDao
) {
    suspend fun shouldIgnore(
        groupId: String,
        packet: SparrowPacket
    ): Boolean {
        if (packet is GroupInvitePacket) return false
        if (chatDao.hasMessageWithTransportMode(groupId, GroupMembershipMessageFactory.LOCAL_CONVERSATION_DELETED_TRANSPORT_MODE)) {
            return true
        }
        if (packet is GroupConversationDeletedPacket) return false
        return groupInvitationDao.findByGroupId(groupId).any { it.status == GroupInvitationStatus.GROUP_DELETED.name }
    }
}
