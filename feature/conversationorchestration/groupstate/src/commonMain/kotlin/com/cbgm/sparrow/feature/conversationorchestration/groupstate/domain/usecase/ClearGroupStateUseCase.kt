package com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.usecase

import com.cbgm.sparrow.core.result.safeSuspendCall
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.repository.GroupAvatarRepository
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.repository.GroupDescriptionRepository
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.repository.GroupPinRepository
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.repository.GroupTitleRepository
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.repository.ExpenseReceiptRepository

/** Removes the locally stored title, description, avatar, pin and expense receipts of a deleted group. */
class ClearGroupStateUseCase(
    private val titleRepository: GroupTitleRepository,
    private val descriptionRepository: GroupDescriptionRepository,
    private val avatarRepository: GroupAvatarRepository,
    private val pinRepository: GroupPinRepository,
    private val expenseReceipts: ExpenseReceiptRepository
) {
    suspend operator fun invoke(groupId: String): Result<Unit> =
        safeSuspendCall {
            avatarRepository.deleteLocal(groupId).getOrThrow()
            titleRepository.deleteLocal(groupId).getOrThrow()
            descriptionRepository.deleteLocal(groupId).getOrThrow()
            pinRepository.deleteLocal(groupId).getOrThrow()
            expenseReceipts.deleteGroup(groupId).getOrThrow()
        }
}
