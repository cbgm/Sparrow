package com.cbgm.sparrow.feature.membership.domain.usecase

import com.cbgm.sparrow.feature.membership.domain.repository.MembershipRepository

class ConfirmGroupMembershipIdentityUseCase(
    private val repository: MembershipRepository
) {
    suspend operator fun invoke(
        sourceId: String,
        updatedAtEpochMilliseconds: Long,
        context: com.cbgm.sparrow.feature.membership.domain.model.GroupMembershipContext,
        memberEncryptionPublicKey: ByteArray,
        memberSigningPublicKey: ByteArray
    ): Result<Unit> =
        repository.confirmJoinIdentity(
            sourceId,
            updatedAtEpochMilliseconds,
            context,
            memberEncryptionPublicKey,
            memberSigningPublicKey
        )
}
