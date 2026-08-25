package com.cbgm.sparrow.feature.chats.data.group.incoming.handler

import com.cbgm.sparrow.core.protocol.handler.IncomingPacketContext
import com.cbgm.sparrow.core.protocol.packet.GroupAvatarUpdatedPacket
import com.cbgm.sparrow.core.protocol.packet.SparrowPacket
import com.cbgm.sparrow.data.database.dao.GroupSecurityDao
import com.cbgm.sparrow.feature.chats.data.group.avatar.GroupAvatarPacketProtocol
import com.cbgm.sparrow.feature.chats.data.group.datasource.GroupAvatarDataSource
import com.cbgm.sparrow.feature.chats.data.group.security.isGroupAdminRole

class GroupAvatarUpdatedPacketHandler internal constructor(
    private val groupSecurityDao: GroupSecurityDao,
    private val packetProtocol: GroupAvatarPacketProtocol,
    private val dataSource: GroupAvatarDataSource
) : GroupPacketHandler {
    override fun canHandle(packet: SparrowPacket): Boolean = packet is GroupAvatarUpdatedPacket

    override suspend fun handle(
        context: IncomingPacketContext,
        packet: SparrowPacket
    ): Result<Unit> =
        runCatching {
            val update = packet as GroupAvatarUpdatedPacket
            packetProtocol.verify(update).getOrThrow()

            val state = groupSecurityDao.findState(update.groupId) ?: error("Group security state was not found")
            if (update.epoch < state.currentEpoch) return@runCatching
            check(update.epoch == state.currentEpoch) { "Group avatar update belongs to a future group epoch" }

            val admin =
                groupSecurityDao
                    .findMemberKeys(update.groupId, update.epoch)
                    .firstOrNull { member ->
                        member.contactId == context.contactId &&
                            member.role.isGroupAdminRole() &&
                            member.signingPublicKey.contentEquals(update.adminSigningPublicKey)
                    } ?: error("Group avatar update was not signed by an active group admin")
            check(admin.contactId == context.contactId)

            val current = dataSource.get(update.groupId)
            if (update.avatar.changedAtEpochMilliseconds <= current.changedAtEpochMilliseconds) {
                return@runCatching
            }

            val payload = update.avatar.payload
            if (payload == null) {
                dataSource.remove(update.groupId, update.avatar.changedAtEpochMilliseconds)
            } else {
                dataSource.save(
                    groupId = update.groupId,
                    bytes = payload.bytes,
                    changedAtEpochMilliseconds = update.avatar.changedAtEpochMilliseconds
                )
            }
        }
}
