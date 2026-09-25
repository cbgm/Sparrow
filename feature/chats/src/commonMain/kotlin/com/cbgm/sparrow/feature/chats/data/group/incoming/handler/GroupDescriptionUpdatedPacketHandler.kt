package com.cbgm.sparrow.feature.chats.data.group.incoming.handler

import com.cbgm.sparrow.core.protocol.handler.IncomingPacketContext
import com.cbgm.sparrow.core.protocol.packet.GroupDescriptionUpdatedPacket
import com.cbgm.sparrow.core.protocol.packet.SparrowPacket
import com.cbgm.sparrow.feature.chats.data.group.datasource.GroupDescriptionDataSource
import com.cbgm.sparrow.feature.chats.data.group.description.GroupDescriptionPacketProtocol
import com.cbgm.sparrow.feature.membership.domain.usecase.AuthorizeGroupMetadataUseCase

class GroupDescriptionUpdatedPacketHandler internal constructor(
    private val authorizeGroupMetadata: AuthorizeGroupMetadataUseCase,
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

            if (!authorizeGroupMetadata.receive(
                    groupId = update.groupId,
                    epoch = update.epoch,
                    contactId = context.contactId,
                    adminSigningPublicKey = update.adminSigningPublicKey,
                    action = "description"
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
                description = update.description,
                changedAtEpochMilliseconds = update.changedAtEpochMilliseconds
            )
        }
}
