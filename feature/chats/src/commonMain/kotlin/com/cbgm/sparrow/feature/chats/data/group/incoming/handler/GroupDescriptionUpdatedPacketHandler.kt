package com.cbgm.sparrow.feature.chats.data.group.incoming.handler

import com.cbgm.sparrow.core.protocol.handler.IncomingPacketContext
import com.cbgm.sparrow.core.protocol.packet.GroupDescriptionUpdatedPacket
import com.cbgm.sparrow.core.protocol.packet.SparrowPacket
import com.cbgm.sparrow.data.database.dao.GroupSecurityDao
import com.cbgm.sparrow.feature.chats.data.group.datasource.GroupDescriptionDataSource
import com.cbgm.sparrow.feature.chats.data.group.description.GroupDescriptionPacketProtocol
import com.cbgm.sparrow.feature.membership.data.model.isGroupAdminRole

class GroupDescriptionUpdatedPacketHandler internal constructor(
    private val groupSecurityDao: GroupSecurityDao,
    private val packetProtocol: GroupDescriptionPacketProtocol,
    private val dataSource: GroupDescriptionDataSource
) : GroupPacketHandler {
    override fun canHandle(packet: SparrowPacket): Boolean = packet is GroupDescriptionUpdatedPacket

    override suspend fun handle(
        context: IncomingPacketContext,
        packet: SparrowPacket
    ): Result<Unit> =
        runCatching {
            val update = packet as GroupDescriptionUpdatedPacket
            packetProtocol.verify(update).getOrThrow()

            val state = groupSecurityDao.findState(update.groupId) ?: error("Group security state was not found")
            if (update.epoch < state.currentEpoch) return@runCatching
            check(update.epoch == state.currentEpoch) { "Group description update belongs to a future group epoch" }

            val admin =
                groupSecurityDao
                    .findMemberKeys(update.groupId, update.epoch)
                    .firstOrNull { member ->
                        member.contactId == context.contactId &&
                            member.role.isGroupAdminRole() &&
                            member.signingPublicKey.contentEquals(update.adminSigningPublicKey)
                    } ?: error("Group description update was not signed by an active group admin")
            check(admin.contactId == context.contactId)

            val current = dataSource.get(update.groupId)
            if (update.changedAtEpochMilliseconds <= current.changedAtEpochMilliseconds) {
                return@runCatching
            }

            dataSource.save(
                groupId = update.groupId,
                description = update.description,
                changedAtEpochMilliseconds = update.changedAtEpochMilliseconds
            )
        }
}
