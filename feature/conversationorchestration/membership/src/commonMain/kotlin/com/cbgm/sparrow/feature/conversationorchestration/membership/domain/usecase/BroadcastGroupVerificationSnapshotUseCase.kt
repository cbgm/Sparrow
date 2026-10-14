package com.cbgm.sparrow.feature.conversationorchestration.membership.domain.usecase

import com.cbgm.sparrow.core.result.safeSuspendCall
import com.cbgm.sparrow.feature.conversationorchestration.membership.domain.model.GroupVerificationStatus
import com.cbgm.sparrow.feature.conversationorchestration.membership.domain.repository.GroupVerificationRepository

/** The admin sends its verification rows to every active member of the current epoch. */
class BroadcastGroupVerificationSnapshotUseCase(
    private val verificationRepository: GroupVerificationRepository,
    private val verificationInputs: GroupVerificationInputsUseCase,
    private val sendGroupVerificationSnapshot: SendGroupVerificationSnapshotUseCase
) {
    suspend operator fun invoke(groupId: String): Result<Unit> =
        safeSuspendCall(logFailure = false) {
            val currentMemberIds =
                verificationInputs.membershipContext(groupId).getOrThrow()
                    .memberKeys.mapTo(mutableSetOf()) { it.contactId }
            verificationRepository.findRows(groupId).getOrThrow()
                .filter { row -> row.status == GroupVerificationStatus.ACTIVE }
                .mapNotNull { row -> row.contactId }
                .filter { contactId -> contactId in currentMemberIds }
                .forEach { contactId -> sendGroupVerificationSnapshot(groupId, contactId).getOrThrow() }
        }
}
