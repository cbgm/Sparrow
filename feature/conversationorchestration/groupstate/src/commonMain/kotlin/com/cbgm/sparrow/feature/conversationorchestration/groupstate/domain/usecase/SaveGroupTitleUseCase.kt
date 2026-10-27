package com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.usecase

import com.cbgm.sparrow.core.base.group.domain.model.GroupMetadataPolicy
import com.cbgm.sparrow.core.util.result.safeSuspendCall
import com.cbgm.sparrow.core.util.time.SystemClock
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.model.GroupStateVersionPolicy
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.model.GroupTitleState
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.repository.GroupTitleRepository

/** Saves a locally changed group title as a new version; returns null when the title did not change. */
class SaveGroupTitleUseCase(
    private val titleRepository: GroupTitleRepository
) {
    suspend operator fun invoke(groupId: String, title: String): Result<GroupTitleState?> =
        safeSuspendCall(logFailure = false) {
            val normalized = GroupMetadataPolicy.normalizeTitle(title)
            val previous = titleRepository.get(groupId).getOrThrow()
            if (previous.title == normalized) return@safeSuspendCall null
            val saved =
                GroupTitleState(
                    groupId = groupId,
                    title = normalized,
                    changedAtEpochMilliseconds = GroupStateVersionPolicy.nextChangedAt(
                        previousChangedAtEpochMilliseconds = previous.changedAtEpochMilliseconds,
                        nowEpochMilliseconds = SystemClock.nowEpochMilliseconds()
                    )
                )
            titleRepository.save(saved).getOrThrow()
            saved
        }
}
