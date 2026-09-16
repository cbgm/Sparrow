package com.cbgm.sparrow.feature.chats.data.group.incoming.handler

import com.cbgm.sparrow.core.protocol.handler.IncomingPacketContext
import com.cbgm.sparrow.core.protocol.message.GroupMessageContentCodec
import com.cbgm.sparrow.core.protocol.packet.GroupPinUpdatedPacket
import com.cbgm.sparrow.core.protocol.packet.SparrowPacket
import com.cbgm.sparrow.data.database.dao.ContactDao
import com.cbgm.sparrow.data.database.dao.GroupSecurityDao
import com.cbgm.sparrow.feature.chats.data.group.datasource.GroupPinDataSource
import com.cbgm.sparrow.feature.chats.data.group.mapper.toEntity
import com.cbgm.sparrow.feature.chats.data.group.pin.GroupPinPacketProtocol
import com.cbgm.sparrow.feature.membership.data.model.isGroupAdminRole

class GroupPinUpdatedPacketHandler internal constructor(
    private val groupSecurityDao: GroupSecurityDao,
    private val contactDao: ContactDao,
    private val packetProtocol: GroupPinPacketProtocol,
    private val dataSource: GroupPinDataSource,
    private val groupMessageContentCodec: GroupMessageContentCodec
) : GroupPacketHandler {
    override fun canHandle(packet: SparrowPacket): Boolean = packet is GroupPinUpdatedPacket

    override suspend fun handle(
        context: IncomingPacketContext,
        packet: SparrowPacket
    ): Result<Unit> =
        runCatching {
            val update = packet as GroupPinUpdatedPacket
            packetProtocol.verify(update).getOrThrow()

            val state = groupSecurityDao.findState(update.groupId) ?: error("Group security state was not found")
            if (update.epoch < state.currentEpoch) return@runCatching
            check(update.epoch == state.currentEpoch) { "Group pin update belongs to a future group epoch" }

            val admin =
                groupSecurityDao
                    .findMemberKeys(update.groupId, update.epoch)
                    .firstOrNull { member ->
                        member.contactId == context.contactId &&
                            member.role.isGroupAdminRole() &&
                            member.signingPublicKey.contentEquals(update.adminSigningPublicKey)
                    } ?: error("Group pin update was not signed by an active group admin")
            check(admin.contactId == context.contactId)

            val current = dataSource.get(update.groupId)
            if (update.changedAtEpochMilliseconds <= (current?.changedAtEpochMilliseconds ?: 0L)) {
                return@runCatching
            }

            val senderKey = update.messageSenderSigningPublicKey
            val isMine = update.hasPinnedMessage && senderKey.contentEquals(state.localSigningPublicKey)
            val senderContactId =
                if (!update.hasPinnedMessage || isMine) {
                    null
                } else {
                    groupSecurityDao
                        .findMemberKeys(update.groupId, update.epoch)
                        .firstOrNull { member -> member.signingPublicKey.contentEquals(senderKey) }
                        ?.contactId
                        ?: contactDao.findBySigningPublicKey(senderKey)?.contact?.id
                }

            dataSource.save(
                update.toEntity(
                    senderContactId = senderContactId,
                    isMine = isMine,
                    groupMessageContentCodec = groupMessageContentCodec
                )
            )
        }
}
