package com.cbgm.sparrow.feature.membership.domain.usecase

import com.cbgm.sparrow.feature.membership.domain.repository.GroupMembershipRepository
import com.cbgm.sparrow.protocol.packet.GroupMemberRemovedPacket

class AuthorizeIncomingGroupRemovalUseCase(
    private val repository: GroupMembershipRepository
) {
    suspend operator fun invoke(
        packet: GroupMemberRemovedPacket,
        senderContactId: String,
        pendingOwnerSigningPublicKey: ByteArray?
    ): Result<Boolean> = repository.authorizeIncomingGroupRemoval(
        packet,
        senderContactId,
        pendingOwnerSigningPublicKey
    )
}
