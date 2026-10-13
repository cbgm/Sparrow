package com.cbgm.sparrow.feature.conversationorchestration.membership.domain.usecase

import com.cbgm.sparrow.feature.conversationorchestration.membership.domain.repository.GroupConversationRepository
import com.cbgm.sparrow.feature.membership.domain.usecase.ConfirmGroupMembershipIdentityUseCase

/** The owner binds the member's accepted keys and phone number to the membership so the welcome can follow. */
class ConfirmGroupMemberIdentityUseCase(
    private val conversationRepository: GroupConversationRepository,
    private val requireMemberPhoneNumber: RequireMemberPhoneNumberUseCase,
    private val confirmGroupMembershipIdentity: ConfirmGroupMembershipIdentityUseCase
) {
    @Suppress("LongParameterList")
    suspend operator fun invoke(
        sourceId: String,
        groupId: String,
        memberContactId: String,
        memberEncryptionPublicKey: ByteArray,
        memberSigningPublicKey: ByteArray,
        updatedAtEpochMilliseconds: Long
    ): Result<Unit> =
        runCatching {
            confirmGroupMembershipIdentity(
                sourceId = sourceId,
                updatedAtEpochMilliseconds = updatedAtEpochMilliseconds,
                context = conversationRepository.getMembershipContext(groupId).getOrThrow(),
                memberEncryptionPublicKey = memberEncryptionPublicKey,
                memberSigningPublicKey = memberSigningPublicKey,
                memberPhoneNumber = requireMemberPhoneNumber(memberContactId).getOrThrow()
            ).getOrThrow()
        }
}
