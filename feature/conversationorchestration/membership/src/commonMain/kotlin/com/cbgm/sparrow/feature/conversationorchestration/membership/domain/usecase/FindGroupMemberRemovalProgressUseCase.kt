package com.cbgm.sparrow.feature.conversationorchestration.membership.domain.usecase

import com.cbgm.sparrow.core.util.result.safeSuspendCall
import com.cbgm.sparrow.feature.conversationorchestration.membership.domain.model.GroupMemberRemovalProgress
import com.cbgm.sparrow.feature.conversationorchestration.membership.domain.repository.GroupConversationRepository
import com.cbgm.sparrow.feature.membership.domain.usecase.FindCommittedGroupMemberRemovalUseCase

/** How far an earlier removal of [contactId] got ([GroupMemberRemovalProgress]). */
class FindGroupMemberRemovalProgressUseCase(
    private val findCommittedGroupMemberRemoval: FindCommittedGroupMemberRemovalUseCase,
    private val conversationRepository: GroupConversationRepository
) {
    suspend operator fun invoke(groupId: String, contactId: String): Result<GroupMemberRemovalProgress> =
        safeSuspendCall(logFailure = false) {
            val committed = findCommittedGroupMemberRemoval(groupId, contactId).getOrThrow()
                ?: return@safeSuspendCall GroupMemberRemovalProgress.NotStarted
            GroupMemberRemovalProgress.of(
                committed = committed,
                isParticipant = conversationRepository.isParticipant(groupId, contactId).getOrThrow()
            )
        }
}
