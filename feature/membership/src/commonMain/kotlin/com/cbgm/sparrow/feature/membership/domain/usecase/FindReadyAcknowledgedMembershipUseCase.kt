package com.cbgm.sparrow.feature.membership.domain.usecase

import com.cbgm.sparrow.core.util.result.safeSuspendCall
import com.cbgm.sparrow.feature.membership.domain.model.GroupWelcomePacketIds
import com.cbgm.sparrow.feature.membership.domain.model.MembershipHandshake
import com.cbgm.sparrow.feature.membership.domain.repository.MembershipRepository
import com.cbgm.sparrow.protocol.packet.GroupReadyAcknowledgementPacket

/**
 * The membership a ready acknowledgement activated, or null when its welcome belongs to no
 * membership of [memberContactId] that is active in the group.
 */
class FindReadyAcknowledgedMembershipUseCase(
    private val repository: MembershipRepository
) {
    suspend operator fun invoke(
        memberContactId: String,
        packet: GroupReadyAcknowledgementPacket
    ): Result<MembershipHandshake?> =
        safeSuspendCall(logFailure = false) {
            GroupWelcomePacketIds.sourceIdOf(packet.welcomePacketId, packet.groupId, packet.epoch)
                ?.let { sourceId -> repository.getHandshake(sourceId).getOrThrow() }
                ?.takeIf { handshake -> handshake.isActiveMembershipOf(packet.groupId, memberContactId) }
        }
}
