package com.cbgm.sparrow.feature.membership.domain.usecase

import com.cbgm.sparrow.feature.membership.domain.repository.GroupMembershipRepository

/** Membership owns the installed epoch keys; orchestration chooses the packet policy. */
class ResolveGroupTransportEncryptionPublicKeyUseCase(
    private val repository: GroupMembershipRepository
) {
    suspend operator fun invoke(
        groupId: String,
        contactId: String,
        useLatestMemberKey: Boolean
    ): Result<ByteArray?> =
        repository.resolveTransportEncryptionPublicKey(groupId, contactId, useLatestMemberKey)
}
