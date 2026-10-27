package com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.usecase

import com.cbgm.sparrow.core.base.group.domain.model.GroupMetadataPolicy
import com.cbgm.sparrow.core.util.result.safeSuspendCall
import com.cbgm.sparrow.core.util.time.SystemClock
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.model.GroupDescriptionState
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.model.GroupStateVersionPolicy
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.repository.GroupDescriptionRepository

/** Saves a locally changed group description as a new version; returns null when it did not change. */
class SaveGroupDescriptionUseCase(
    private val descriptionRepository: GroupDescriptionRepository
) {
    suspend operator fun invoke(groupId: String, description: String): Result<GroupDescriptionState?> =
        safeSuspendCall(logFailure = false) {
            val normalized = GroupMetadataPolicy.normalizeDescription(description)
            val previous = descriptionRepository.get(groupId).getOrThrow()
            if (previous.description == normalized) return@safeSuspendCall null
            val saved =
                GroupDescriptionState(
                    groupId = groupId,
                    description = normalized,
                    changedAtEpochMilliseconds = GroupStateVersionPolicy.nextChangedAt(
                        previousChangedAtEpochMilliseconds = previous.changedAtEpochMilliseconds,
                        nowEpochMilliseconds = SystemClock.nowEpochMilliseconds()
                    )
                )
            descriptionRepository.save(saved).getOrThrow()
            saved
        }
}
