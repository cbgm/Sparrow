package com.cbgm.sparrow.feature.chats.data.group.incoming.handler

import com.cbgm.sparrow.core.logging.SparrowLog
import com.cbgm.sparrow.core.protocol.handler.IncomingPacketContext
import com.cbgm.sparrow.core.protocol.packet.GroupJoinRequestPacket
import com.cbgm.sparrow.core.protocol.packet.SparrowPacket
import com.cbgm.sparrow.core.protocol.profile.RemoteProfilePictureMetadataProcessor
import com.cbgm.sparrow.feature.chats.data.group.incoming.GroupJoinRequestIncomingProcessor

internal class GroupJoinRequestPacketHandler(
    private val joinRequestIncomingProcessor: GroupJoinRequestIncomingProcessor,
    private val remoteProfilePictureMetadataProcessor: RemoteProfilePictureMetadataProcessor
) : GroupPacketHandler {
    private val logger = SparrowLog.withTag("GroupJoinRequestPacketHandler")

    override fun canHandle(packet: SparrowPacket): Boolean = packet is GroupJoinRequestPacket

    override suspend fun handle(
        context: IncomingPacketContext,
        packet: SparrowPacket
    ): Result<Unit> =
        runCatching {
            val joinRequest = packet as GroupJoinRequestPacket
            joinRequestIncomingProcessor
                .process(
                    memberContactId = context.contactId,
                    packet = joinRequest,
                    receivedAtEpochMilliseconds = context.receivedAtEpochMilliseconds
                ).getOrThrow()

            remoteProfilePictureMetadataProcessor
                .apply(context.contactId, joinRequest.profilePicture)
                .onFailure { error ->
                    logger.warn(error) { "Could not store profile picture for ${context.contactId}" }
                }
        }
}
