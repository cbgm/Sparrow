package com.cbgm.sparrow.feature.membership.domain.usecase

import com.cbgm.sparrow.feature.membership.domain.repository.GroupMembershipRepository
import com.cbgm.sparrow.protocol.packet.GroupConversationDeletedPacket

class AuthorizeIncomingGroupDeletionUseCase(
    private val repository: GroupMembershipRepository
) {
    suspend operator fun invoke(
        packet: GroupConversationDeletedPacket,
        ownerContactId: String,
        ownerSigningPublicKey: ByteArray
    ): Result<Unit> = repository.authorizeIncomingGroupDeletion(packet, ownerContactId, ownerSigningPublicKey)
}
