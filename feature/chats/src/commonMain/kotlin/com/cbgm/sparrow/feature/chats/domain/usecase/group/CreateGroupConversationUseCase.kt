package com.cbgm.sparrow.feature.chats.domain.usecase.group

import com.cbgm.sparrow.feature.chats.domain.repository.group.GroupConversationRepository
import com.cbgm.sparrow.feature.invite.domain.model.InvitationPayloadType
import com.cbgm.sparrow.feature.invite.domain.usecase.SendInvitationUseCase
import com.cbgm.sparrow.feature.membership.domain.usecase.InitializeOwnedGroupMembershipUseCase

class CreateGroupConversationUseCase(
    private val repository: GroupConversationRepository,
    private val initializeOwnedGroupMembership: InitializeOwnedGroupMembershipUseCase,
    private val sendInvitation: SendInvitationUseCase
) {
    suspend operator fun invoke(
        title: String,
        contactIds: Set<String>
    ): Result<String> =
        runCatching {
            require(contactIds.isNotEmpty()) { "A group requires at least one contact" }
            val groupId = repository.create(title).getOrThrow()
            initializeOwnedGroupMembership(groupId).getOrThrow()
            sendInvitation(
                payloadType = InvitationPayloadType.GROUP,
                payloadId = groupId,
                peerIds = contactIds
            ).getOrThrow()
            groupId
        }
}
