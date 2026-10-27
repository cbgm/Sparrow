package com.cbgm.sparrow.feature.membership.domain.usecase

import com.cbgm.sparrow.core.util.result.safeSuspendCall
import com.cbgm.sparrow.feature.membership.domain.repository.GroupMembershipRepository
import com.cbgm.sparrow.protocol.packet.GroupReadyAcknowledgementPacket

/**
 * The owner discards old epoch keys on rotation, so a ready acknowledgement of an older epoch is obsolete and
 * changes nothing. A future epoch is not obsolete here; Membership rejects it when it receives the packet.
 */
class CheckReadyAcknowledgementEpochUseCase(
    private val repository: GroupMembershipRepository
) {
    suspend fun isObsolete(packet: GroupReadyAcknowledgementPacket): Result<Boolean> =
        safeSuspendCall(logFailure = false) { packet.epoch < repository.getCurrentEpoch(packet.groupId).getOrThrow() }

    /** Whether the acknowledged epoch was not rotated while Membership processed the acknowledgement. */
    suspend fun isStillCurrent(packet: GroupReadyAcknowledgementPacket): Result<Boolean> =
        safeSuspendCall(logFailure = false) { packet.epoch == repository.getCurrentEpoch(packet.groupId).getOrThrow() }
}
