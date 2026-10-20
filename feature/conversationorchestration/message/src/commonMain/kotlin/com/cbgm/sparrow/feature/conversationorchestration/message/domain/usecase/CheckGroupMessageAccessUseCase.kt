package com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase

import com.cbgm.sparrow.core.base.message.domain.model.MessageTransportMode
import com.cbgm.sparrow.core.util.result.safeSuspendCall
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.model.GroupMessageAccess
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.repository.GroupMessageRepository
import com.cbgm.sparrow.feature.membership.domain.usecase.GetGroupMessageMembershipAccessUseCase

/** Decides whether the local member may currently write to a group conversation. */
class CheckGroupMessageAccessUseCase(
    private val groupMessageRepository: GroupMessageRepository,
    private val getMessageMembershipAccess: GetGroupMessageMembershipAccessUseCase
) {
    suspend operator fun invoke(groupId: String): Result<GroupMessageAccess> =
        safeSuspendCall(logFailure = false) {
            val conversation = groupMessageRepository.findConversation(groupId).getOrThrow()
                ?: error("Group conversation was not found")
            check(conversation.isGroup) { "Conversation is not a group" }

            val access = getMessageMembershipAccess(groupId).getOrThrow()
            when {
                access.isJoinPending -> GroupMessageAccess.Denied("Complete the group join before sending messages")
                access.isLeavePending ->
                    GroupMessageAccess.Denied("Messages are disabled while the group is being left")
                access.isDeleted -> GroupMessageAccess.Denied("This group conversation was deleted")
                !access.hasCurrentMembership && hasEndedLocalMembership(groupId) ->
                    GroupMessageAccess.Denied("You are no longer a member of this group")
                else -> GroupMessageAccess.Granted
            }
        }

    private suspend fun hasEndedLocalMembership(groupId: String): Boolean =
        groupMessageRepository
            .hasMessageWithTransportMode(groupId, MessageTransportMode.LOCAL_GROUP_MEMBERSHIP_REMOVED)
            .getOrThrow() ||
            groupMessageRepository
                .hasMessageWithTransportMode(groupId, MessageTransportMode.LOCAL_GROUP_MEMBERSHIP_LEFT)
                .getOrThrow()
}
