package com.cbgm.sparrow.feature.chats.data.group.incoming.handler

import com.cbgm.sparrow.core.protocol.handler.IncomingPacketContext
import com.cbgm.sparrow.core.protocol.message.GroupMessageContentCodec
import com.cbgm.sparrow.core.protocol.packet.GroupPinUpdatedPacket
import com.cbgm.sparrow.core.protocol.packet.SparrowPacket
import com.cbgm.sparrow.feature.chats.data.group.datasource.GroupPinDataSource
import com.cbgm.sparrow.feature.chats.data.group.mapper.toEntity
import com.cbgm.sparrow.feature.chats.data.group.pin.GroupPinPacketProtocol
import com.cbgm.sparrow.feature.identity.domain.usecase.FindRemoteIdentityPeerIdUseCase
import com.cbgm.sparrow.feature.membership.domain.usecase.AuthorizeGroupMetadataUseCase

class GroupPinUpdatedPacketHandler internal constructor(
    private val authorizeGroupMetadata: AuthorizeGroupMetadataUseCase,
    private val findRemoteIdentityPeerId: FindRemoteIdentityPeerIdUseCase,
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

            if (!authorizeGroupMetadata.receive(
                    groupId = update.groupId,
                    epoch = update.epoch,
                    contactId = context.contactId,
                    adminSigningPublicKey = update.adminSigningPublicKey,
                    action = "pin"
                ).getOrThrow()
            ) {
                return@runCatching
            }

            val current = dataSource.get(update.groupId)
            if (update.changedAtEpochMilliseconds <= (current?.changedAtEpochMilliseconds ?: 0L)) {
                return@runCatching
            }

            val senderKey = update.messageSenderSigningPublicKey
            val messageSender =
                if (update.hasPinnedMessage) {
                    authorizeGroupMetadata.resolveMessageSender(
                        groupId = update.groupId,
                        epoch = update.epoch,
                        signingPublicKey = senderKey
                    ).getOrThrow()
                } else {
                    null
                }
            val isMine = messageSender?.isLocal == true
            val senderContactId =
                if (!update.hasPinnedMessage || isMine) {
                    null
                } else {
                    messageSender?.memberContactId
                        ?: findRemoteIdentityPeerId(senderKey).getOrThrow()
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
