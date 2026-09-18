package com.cbgm.sparrow.feature.invite.data.group

import com.cbgm.sparrow.core.protocol.packet.GroupInvitePacket
import com.cbgm.sparrow.feature.invite.data.protocol.GroupInvitationPacketProcessor
import com.cbgm.sparrow.feature.invite.domain.model.InvitationDirection
import com.cbgm.sparrow.feature.invite.domain.model.InvitationLifecycleRecord
import com.cbgm.sparrow.feature.invite.domain.model.InvitationPayloadType
import com.cbgm.sparrow.feature.invite.domain.usecase.RecordPendingInvitationUseCase
import com.cbgm.sparrow.feature.invite.domain.usecase.ShouldRecordPendingInvitationUseCase

internal class GroupInviteIncomingProcessor(
    private val processor: GroupInvitationPacketProcessor,
    private val shouldRecordPendingInvitation: ShouldRecordPendingInvitationUseCase,
    private val recordPendingInvitation: RecordPendingInvitationUseCase
) {
    suspend fun process(
        ownerContactId: String,
        packet: GroupInvitePacket,
        receivedAtEpochMilliseconds: Long
    ): Result<Unit> =
        runCatching {
            val record =
                InvitationLifecycleRecord(
                    invitationId = packet.invitationId,
                    payloadType = InvitationPayloadType.GROUP,
                    payloadId = packet.groupId,
                    peerId = ownerContactId,
                    direction = InvitationDirection.INCOMING,
                    createdAtEpochMilliseconds = packet.createdAtEpochMilliseconds,
                    expiresAtEpochMilliseconds = packet.expiresAtEpochMilliseconds,
                    updatedAtEpochMilliseconds =
                        maxOf(packet.createdAtEpochMilliseconds, receivedAtEpochMilliseconds)
                )
            val shouldStage = shouldRecordPendingInvitation(record).getOrThrow()
            processor
                .receiveInvite(
                    ownerContactId = ownerContactId,
                    packet = packet,
                    receivedAtEpochMilliseconds = receivedAtEpochMilliseconds,
                    shouldStage = shouldStage
                ).getOrThrow()
            if (shouldStage) {
                recordPendingInvitation(record).getOrThrow()
            }
        }
}
