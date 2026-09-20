package com.cbgm.sparrow.feature.chats.data.group.incoming.handler

import com.cbgm.sparrow.core.protocol.handler.IncomingPacketContext
import com.cbgm.sparrow.core.protocol.packet.GroupTitleUpdatedPacket
import com.cbgm.sparrow.core.protocol.packet.SparrowPacket
import com.cbgm.sparrow.feature.chats.data.group.datasource.GroupTitleDataSource
import com.cbgm.sparrow.feature.chats.data.group.title.GroupTitlePacketProtocol
import com.cbgm.sparrow.feature.membership.domain.usecase.AuthorizeGroupMetadataUseCase

class GroupTitleUpdatedPacketHandler internal constructor(
    private val authorizeGroupMetadata: AuthorizeGroupMetadataUseCase,
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

            if (!authorizeGroupMetadata.receive(
                    groupId = update.groupId,
                    epoch = update.epoch,
                    contactId = context.contactId,
                    adminSigningPublicKey = update.adminSigningPublicKey,
                    action = "title"
                ).getOrThrow()
            ) {
                return@runCatching
            }

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
