package com.cbgm.sparrow.feature.membership.domain.usecase

import com.cbgm.sparrow.core.protocol.packet.GroupMemberActivatedPacket
import com.cbgm.sparrow.feature.membership.domain.repository.GroupMembershipRepository

class AuthorizeIncomingGroupActivationUseCase(
    private val repository: GroupMembershipRepository
) {
    suspend operator fun invoke(
        packet: GroupMemberActivatedPacket,
        ownerContactId: String,
        ownerSigningPublicKey: ByteArray,
        transportMode: String
    ): Result<Boolean> = repository.authorizeIncomingActivation(
        packet,
        ownerContactId,
        ownerSigningPublicKey,
        transportMode
    )
}
