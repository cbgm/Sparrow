package com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.usecase

import com.cbgm.sparrow.core.base.group.domain.model.GroupMetadataPolicy
import com.cbgm.sparrow.core.util.result.safeSuspendCall
import com.cbgm.sparrow.core.util.time.SystemClock
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.model.GroupStateVersionPolicy
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.model.GroupTitleState
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.repository.GroupTitleRepository

/**
 * The next, still unpublished version of a locally changed group title, or null when the title did not change.
 * An unchanged title whose earlier change was never published is returned as it is, so it is published again.
 */
class PrepareGroupTitleUseCase(
    private val titleRepository: GroupTitleRepository
) {
    suspend operator fun invoke(groupId: String, title: String): Result<GroupTitleState?> =
        safeSuspendCall(logFailure = false) {
            val normalized = GroupMetadataPolicy.normalizeTitle(title)
            val previous = titleRepository.get(groupId).getOrThrow()
            if (previous.title == normalized) return@safeSuspendCall previous.takeIf { it.publishPending }
            GroupTitleState(
                groupId = groupId,
                title = normalized,
                changedAtEpochMilliseconds = GroupStateVersionPolicy.nextChangedAt(
                    previousChangedAtEpochMilliseconds = previous.changedAtEpochMilliseconds,
                    nowEpochMilliseconds = SystemClock.nowEpochMilliseconds()
                ),
                publishPending = true
            )
        }
}
