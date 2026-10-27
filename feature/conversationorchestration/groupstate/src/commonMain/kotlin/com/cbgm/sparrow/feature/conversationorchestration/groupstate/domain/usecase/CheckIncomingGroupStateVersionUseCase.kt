package com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.usecase

import com.cbgm.sparrow.core.util.result.safeSuspendCall
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.model.GroupPinSlotPolicy
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.model.GroupStateVersionPolicy
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.repository.GroupAvatarRepository
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.repository.GroupDescriptionRepository
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.repository.GroupPinRepository
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.repository.GroupTitleRepository

/** Whether a received group state version replaces the stored one ([GroupStateVersionPolicy]). */
class CheckIncomingGroupStateVersionUseCase(
    private val titleRepository: GroupTitleRepository,
    private val descriptionRepository: GroupDescriptionRepository,
    private val avatarRepository: GroupAvatarRepository,
    private val pinRepository: GroupPinRepository
) {
    suspend fun isNewerTitle(groupId: String, changedAtEpochMilliseconds: Long): Result<Boolean> =
        safeSuspendCall(logFailure = false) {
            GroupStateVersionPolicy.isNewer(
                changedAtEpochMilliseconds,
                titleRepository.get(groupId).getOrThrow().changedAtEpochMilliseconds
            )
        }

    suspend fun isNewerDescription(groupId: String, changedAtEpochMilliseconds: Long): Result<Boolean> =
        safeSuspendCall(logFailure = false) {
            GroupStateVersionPolicy.isNewer(
                changedAtEpochMilliseconds,
                descriptionRepository.get(groupId).getOrThrow().changedAtEpochMilliseconds
            )
        }

    suspend fun isNewerAvatar(groupId: String, changedAtEpochMilliseconds: Long): Result<Boolean> =
        safeSuspendCall(logFailure = false) {
            GroupStateVersionPolicy.isNewer(
                changedAtEpochMilliseconds,
                avatarRepository.get(groupId).getOrThrow().changedAtEpochMilliseconds
            )
        }

    /**
     * Whether a received pin of [messageId] (null clears the slot) replaces the stored one: it must be newer, and
     * an open expense board keeps the slot against other messages ([GroupPinSlotPolicy]).
     */
    suspend fun acceptsPin(groupId: String, changedAtEpochMilliseconds: Long, messageId: String?): Result<Boolean> =
        safeSuspendCall(logFailure = false) {
            val current = pinRepository.find(groupId).getOrThrow()
            GroupStateVersionPolicy.isNewer(changedAtEpochMilliseconds, current?.changedAtEpochMilliseconds ?: 0L) &&
                !GroupPinSlotPolicy.expensesKeepSlot(current, messageId)
        }
}
