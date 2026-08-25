package com.cbgm.sparrow.feature.chats.data.group.incoming.handler

import com.cbgm.sparrow.core.logging.SparrowLog
import com.cbgm.sparrow.core.protocol.handler.IncomingPacketContext
import com.cbgm.sparrow.core.protocol.packet.GroupInvitePacket
import com.cbgm.sparrow.core.protocol.packet.SparrowPacket
import com.cbgm.sparrow.core.protocol.profile.RemoteProfilePictureMetadataProcessor
import com.cbgm.sparrow.feature.chats.data.group.incoming.GroupInviteIncomingProcessor

internal class GroupInvitePacketHandler(
    private val inviteIncomingProcessor: GroupInviteIncomingProcessor,
    private val remoteProfilePictureMetadataProcessor: RemoteProfilePictureMetadataProcessor
) : GroupPacketHandler {
    private val logger = SparrowLog.withTag("GroupInvitePacketHandler")

    override fun canHandle(packet: SparrowPacket): Boolean = packet is GroupInvitePacket

    override suspend fun handle(
        context: IncomingPacketContext,
        packet: SparrowPacket
    ): Result<Unit> =
        runCatching {
            val invite = packet as GroupInvitePacket
            inviteIncomingProcessor
                .process(
                    ownerContactId = context.contactId,
                    packet = invite,
                    receivedAtEpochMilliseconds = context.receivedAtEpochMilliseconds
                ).getOrThrow()

            remoteProfilePictureMetadataProcessor
                .apply(context.contactId, invite.profilePicture)
                .onFailure { error ->
                    logger.warn(error) { "Could not store profile picture for ${context.contactId}" }
                }
        }
}
