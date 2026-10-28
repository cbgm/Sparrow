package com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.usecase

import com.cbgm.sparrow.core.base.group.domain.model.GroupMetadataPolicy
import com.cbgm.sparrow.core.util.result.safeSuspendCall
import com.cbgm.sparrow.core.util.time.SystemClock
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.model.GroupDescriptionState
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.model.GroupStateVersionPolicy
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.repository.GroupDescriptionRepository

/**
 * The next, still unpublished version of a locally changed group description, or null when it did not change.
 * An unchanged description whose earlier change was never published is returned as it is, so it is published again.
 */
class PrepareGroupDescriptionUseCase(
    private val descriptionRepository: GroupDescriptionRepository
) {
    suspend operator fun invoke(groupId: String, description: String): Result<GroupDescriptionState?> =
        safeSuspendCall(logFailure = false) {
            val normalized = GroupMetadataPolicy.normalizeDescription(description)
            val previous = descriptionRepository.get(groupId).getOrThrow()
            if (previous.description == normalized) return@safeSuspendCall previous.takeIf { it.publishPending }
            GroupDescriptionState(
                groupId = groupId,
                description = normalized,
                changedAtEpochMilliseconds = GroupStateVersionPolicy.nextChangedAt(
                    previousChangedAtEpochMilliseconds = previous.changedAtEpochMilliseconds,
                    nowEpochMilliseconds = SystemClock.nowEpochMilliseconds()
                ),
                publishPending = true
            )
        }
}
