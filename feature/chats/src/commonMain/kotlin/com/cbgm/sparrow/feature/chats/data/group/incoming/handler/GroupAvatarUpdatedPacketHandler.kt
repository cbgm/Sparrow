package com.cbgm.sparrow.feature.chats.data.group.incoming.handler

import com.cbgm.sparrow.core.protocol.handler.IncomingPacketContext
import com.cbgm.sparrow.core.protocol.packet.GroupAvatarUpdatedPacket
import com.cbgm.sparrow.core.protocol.packet.SparrowPacket
import com.cbgm.sparrow.feature.chats.data.group.avatar.GroupAvatarPacketProtocol
import com.cbgm.sparrow.feature.chats.data.group.datasource.GroupAvatarDataSource
import com.cbgm.sparrow.feature.membership.domain.usecase.AuthorizeGroupMetadataUseCase

class GroupAvatarUpdatedPacketHandler internal constructor(
    private val authorizeGroupMetadata: AuthorizeGroupMetadataUseCase,
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

            if (!authorizeGroupMetadata.receive(
                    groupId = update.groupId,
                    epoch = update.epoch,
                    contactId = context.contactId,
                    adminSigningPublicKey = update.adminSigningPublicKey,
                    action = "avatar"
                ).getOrThrow()
            ) {
                return@runCatching
            }

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
