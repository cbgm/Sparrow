package com.cbgm.sparrow.feature.invite.data.group

import com.cbgm.sparrow.core.protocol.packet.GroupJoinRequestPacket
import com.cbgm.sparrow.feature.invite.data.protocol.GroupInvitationPacketProcessor
import com.cbgm.sparrow.feature.invite.domain.model.InvitationPayloadType
import com.cbgm.sparrow.feature.invite.domain.usecase.HandleInvitationResponseUseCase

internal class GroupJoinRequestIncomingProcessor(
    private val processor: GroupInvitationPacketProcessor,
    private val handleInvitationResponse: HandleInvitationResponseUseCase
) {
    suspend fun process(
        memberContactId: String,
        packet: GroupJoinRequestPacket,
        receivedAtEpochMilliseconds: Long
    ): Result<Unit> =
        runCatching {
            val response =
                processor
                    .receiveJoinRequest(
                        memberContactId = memberContactId,
                        packet = packet,
                        receivedAtEpochMilliseconds = receivedAtEpochMilliseconds
                    ).getOrThrow()
                    ?: return@runCatching

            handleInvitationResponse(
                payloadType = InvitationPayloadType.GROUP,
                invitationId = packet.invitationId,
                response = response,
                applyResponseEffects = { Result.success(Unit) }
            ).getOrThrow()
        }
}
