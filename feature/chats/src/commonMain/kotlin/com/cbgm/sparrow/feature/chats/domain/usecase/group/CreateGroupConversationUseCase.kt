package com.cbgm.sparrow.feature.chats.domain.usecase.group

import com.cbgm.sparrow.feature.chats.domain.repository.group.GroupConversationRepository
import com.cbgm.sparrow.feature.chats.domain.repository.group.GroupVerificationActionRepository
import com.cbgm.sparrow.feature.conversationorchestration.domain.usecase.AddConversationMembersUseCase

class CreateGroupConversationUseCase(
    private val repository: GroupConversationRepository,
    private val verificationRepository: GroupVerificationActionRepository,
    private val addConversationMembers: AddConversationMembersUseCase
) {
    suspend operator fun invoke(
        title: String,
        contactIds: Set<String>
    ): Result<String> =
        runCatching {
            require(contactIds.isNotEmpty()) { "A group requires at least one contact" }
            val groupId = repository.create(title).getOrThrow()
            verificationRepository.initializeOwnedGroup(groupId).getOrThrow()
            addConversationMembers(
                conversationId = groupId,
                peerIds = contactIds
            ).getOrThrow()
            groupId
        }
}
