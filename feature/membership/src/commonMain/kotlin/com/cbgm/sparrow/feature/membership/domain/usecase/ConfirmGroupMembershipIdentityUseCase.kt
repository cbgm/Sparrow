package com.cbgm.sparrow.feature.membership.domain.usecase

import com.cbgm.sparrow.feature.membership.domain.model.GroupMembershipContext
import com.cbgm.sparrow.feature.membership.domain.repository.MembershipRepository

class ConfirmGroupMembershipIdentityUseCase(
    private val repository: MembershipRepository
) {
    suspend operator fun invoke(
        sourceId: String,
        updatedAtEpochMilliseconds: Long,
        context: GroupMembershipContext,
        memberEncryptionPublicKey: ByteArray,
        memberSigningPublicKey: ByteArray,
        memberPhoneNumber: String
    ): Result<Unit> =
        repository.confirmJoinIdentity(
            sourceId,
            updatedAtEpochMilliseconds,
            context,
            memberEncryptionPublicKey,
            memberSigningPublicKey,
            memberPhoneNumber
        )
}
