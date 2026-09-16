package com.cbgm.sparrow.feature.chats.data.group.incoming.handler

import com.cbgm.sparrow.core.protocol.handler.IncomingPacketContext
import com.cbgm.sparrow.core.protocol.packet.GroupTitleUpdatedPacket
import com.cbgm.sparrow.core.protocol.packet.SparrowPacket
import com.cbgm.sparrow.data.database.dao.GroupSecurityDao
import com.cbgm.sparrow.feature.chats.data.group.datasource.GroupTitleDataSource
import com.cbgm.sparrow.feature.chats.data.group.title.GroupTitlePacketProtocol
import com.cbgm.sparrow.feature.membership.data.model.isGroupAdminRole

class GroupTitleUpdatedPacketHandler internal constructor(
    private val groupSecurityDao: GroupSecurityDao,
    private val packetProtocol: GroupTitlePacketProtocol,
    private val dataSource: GroupTitleDataSource
) : GroupPacketHandler {
    override fun canHandle(packet: SparrowPacket): Boolean = packet is GroupTitleUpdatedPacket

    override suspend fun handle(
        context: IncomingPacketContext,
        packet: SparrowPacket
    ): Result<Unit> =
        runCatching {
            val update = packet as GroupTitleUpdatedPacket
            packetProtocol.verify(update).getOrThrow()

            val state = groupSecurityDao.findState(update.groupId) ?: error("Group security state was not found")
            if (update.epoch < state.currentEpoch) return@runCatching
            check(update.epoch == state.currentEpoch) { "Group title update belongs to a future group epoch" }

            val admin =
                groupSecurityDao
                    .findMemberKeys(update.groupId, update.epoch)
                    .firstOrNull { member ->
                        member.contactId == context.contactId &&
                            member.role.isGroupAdminRole() &&
                            member.signingPublicKey.contentEquals(update.adminSigningPublicKey)
                    } ?: error("Group title update was not signed by an active group admin")
            check(admin.contactId == context.contactId)

            val current = dataSource.get(update.groupId)
            if (update.changedAtEpochMilliseconds <= current.changedAtEpochMilliseconds) {
                return@runCatching
            }

            dataSource.save(
                groupId = update.groupId,
                title = update.title,
                changedAtEpochMilliseconds = update.changedAtEpochMilliseconds
            )
        }
}
