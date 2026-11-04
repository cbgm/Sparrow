package com.cbgm.sparrow.feature.membership.domain.usecase

import com.cbgm.sparrow.feature.membership.domain.model.GroupMembershipContext
import com.cbgm.sparrow.feature.membership.domain.model.OpenedIncomingGroupWelcome
import com.cbgm.sparrow.feature.membership.domain.model.isGroupAdminRole
import com.cbgm.sparrow.feature.membership.domain.repository.GroupMembershipRepository
import com.cbgm.sparrow.protocol.packet.GroupCreatedPacket

/**
 * An admin who leaves creates the epoch it leaves with, so it still knows that key. The first admin named in that
 * welcome rotates once more right away; exactly one member does it, so no two rotations race.
 */
class RotateGroupKeyAfterAdminLeftUseCase(
    private val repository: GroupMembershipRepository
) {
    suspend operator fun invoke(welcome: OpenedIncomingGroupWelcome, packet: GroupCreatedPacket): Result<Unit> {
        val continuingAdmin = packet.members.firstOrNull { member -> member.role.isGroupAdminRole() }
        val isLocalAdmin = continuingAdmin?.signingPublicKey?.contentEquals(welcome.localSigningPublicKey) == true
        if (!welcome.authorityLeft || !isLocalAdmin) return Result.success(Unit)
        return repository.rotateAfterAdminLeft(
            groupId = packet.groupId,
            expectedEpoch = packet.epoch,
            context = GroupMembershipContext(packet.title, packet.createdAtEpochMilliseconds)
        )
    }
}
