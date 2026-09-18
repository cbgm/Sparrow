package com.cbgm.sparrow.feature.conversationorchestration.data.group.invitation

import com.cbgm.sparrow.core.protocol.packet.GroupInviteDeclinedPacket
import com.cbgm.sparrow.feature.invite.domain.model.InvitationPayloadType
import com.cbgm.sparrow.feature.invite.domain.usecase.HandleInvitationResponseUseCase

internal class GroupInviteDeclinedIncomingProcessor(
    private val processor: GroupInvitationPacketProcessor,
    private val handleInvitationResponse: HandleInvitationResponseUseCase
) {
    suspend fun process(
        memberContactId: String,
        packet: GroupInviteDeclinedPacket,
        receivedAtEpochMilliseconds: Long
    ): Result<Unit> =
        runCatching {
            val response =
                processor
                    .receiveDeclined(
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
