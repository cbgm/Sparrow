package com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.usecase

import com.cbgm.sparrow.core.util.result.safeSuspendCall
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.model.GroupAvatarState
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.model.GroupDescriptionState
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.model.GroupPinState
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.model.GroupStateKind
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.model.GroupTitleState
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.model.UnpublishedGroupState
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.repository.GroupAvatarRepository
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.repository.GroupDescriptionRepository
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.repository.GroupPinRepository
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.repository.GroupTitleRepository

/**
 * Group state an admin changed locally that is not queued for the members yet. A newer state that arrived from
 * another admin replaced it and counts as published, so a superseded local change is never found here.
 */
class FindUnpublishedGroupStateUseCase(
    private val titleRepository: GroupTitleRepository,
    private val descriptionRepository: GroupDescriptionRepository,
    private val avatarRepository: GroupAvatarRepository,
    private val pinRepository: GroupPinRepository
) {
    /** Every group with unpublished state, so a change interrupted by a restart is published after all. */
    suspend operator fun invoke(): Result<List<UnpublishedGroupState>> =
        safeSuspendCall(logFailure = false) {
            listOf(
                GroupStateKind.TITLE to titleRepository.findPublishPendingGroupIds().getOrThrow(),
                GroupStateKind.DESCRIPTION to descriptionRepository.findPublishPendingGroupIds().getOrThrow(),
                GroupStateKind.AVATAR to avatarRepository.findPublishPendingGroupIds().getOrThrow(),
                GroupStateKind.PIN to pinRepository.findPublishPendingGroupIds().getOrThrow()
            ).flatMap { (kind, groupIds) -> groupIds.map { groupId -> UnpublishedGroupState(groupId, kind) } }
        }

    suspend fun title(groupId: String): Result<GroupTitleState?> =
        safeSuspendCall(logFailure = false) {
            titleRepository.get(groupId).getOrThrow().takeIf { state -> state.publishPending }
        }

    suspend fun description(groupId: String): Result<GroupDescriptionState?> =
        safeSuspendCall(logFailure = false) {
            descriptionRepository.get(groupId).getOrThrow().takeIf { state -> state.publishPending }
        }

    suspend fun avatar(groupId: String): Result<GroupAvatarState?> =
        safeSuspendCall(logFailure = false) {
            avatarRepository.get(groupId).getOrThrow().takeIf { state -> state.publishPending }
        }

    suspend fun pin(groupId: String): Result<GroupPinState?> =
        safeSuspendCall(logFailure = false) {
            pinRepository.find(groupId).getOrThrow()?.takeIf { pin -> pin.publishPending }
        }
}
