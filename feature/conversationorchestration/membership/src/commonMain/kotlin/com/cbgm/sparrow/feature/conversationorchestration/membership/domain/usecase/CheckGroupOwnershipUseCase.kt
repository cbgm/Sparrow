package com.cbgm.sparrow.feature.conversationorchestration.membership.domain.usecase

import com.cbgm.sparrow.core.result.safeSuspendCall
import com.cbgm.sparrow.feature.conversationorchestration.membership.domain.repository.GroupVerificationRepository

/** The local device created this group, even when its security state is gone. */
class CheckGroupOwnershipUseCase(
    private val verificationRepository: GroupVerificationRepository,
    private val verificationInputs: GroupVerificationInputsUseCase
) {
    suspend operator fun invoke(groupId: String): Result<Boolean> =
        safeSuspendCall(logFailure = false) {
            verificationRepository.findRows(groupId).getOrThrow().any { it.contactId != null } ||
                verificationInputs.membershipContext(groupId).getOrThrow().ownsGroup
        }
}
