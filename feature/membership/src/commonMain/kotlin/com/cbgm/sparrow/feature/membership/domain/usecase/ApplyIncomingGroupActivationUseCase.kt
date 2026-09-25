package com.cbgm.sparrow.feature.membership.domain.usecase

import com.cbgm.sparrow.core.protocol.packet.GroupMemberActivatedPacket
import com.cbgm.sparrow.feature.membership.domain.repository.GroupMembershipRepository

class ApplyIncomingGroupActivationUseCase(
    private val repository: GroupMembershipRepository
) {
    suspend operator fun invoke(
        packet: GroupMemberActivatedPacket,
        ownerContactId: String,
        ownerSigningPublicKey: ByteArray,
        transportMode: String,
        memberContactId: String?,
        receivedAtEpochMilliseconds: Long
    ): Result<Boolean> = repository.applyIncomingActivation(
        packet,
        ownerContactId,
        ownerSigningPublicKey,
        transportMode,
        memberContactId,
        receivedAtEpochMilliseconds
    )
}
