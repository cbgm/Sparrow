package com.cbgm.sparrow.feature.conversationorchestration.domain.usecase

import com.cbgm.sparrow.core.protocol.identity.LocalSigningKeyPairProvider
import com.cbgm.sparrow.feature.conversationorchestration.domain.port.ConversationPort
import com.cbgm.sparrow.feature.membership.domain.repository.GroupSecurityRepository

/** Coordinates Chats persistence, Membership epoch initialization, and invites in that order. */
class CreateConversationGroupUseCase internal constructor(
    private val conversations: ConversationPort,
    private val groupSecurity: GroupSecurityRepository,
    private val signingKeys: LocalSigningKeyPairProvider,
    private val addConversationMembers: AddConversationMembersUseCase
) {
    suspend operator fun invoke(title: String, contactIds: Set<String>): Result<String> =
        runCatching {
            require(contactIds.isNotEmpty()) { "A group requires at least one contact" }
            val groupId = conversations.createOwnedGroupConversation(title).getOrThrow()
            // No invitation may be created before the owner's epoch/key exist.
            groupSecurity.initializeOwnedGroup(
                groupId = groupId,
                createdAtEpochMilliseconds = conversations.getGroupMembershipContext(groupId)
                    .getOrThrow().createdAtEpochMilliseconds,
                localSigningKeyPair = signingKeys.getSigningKeyPair().getOrThrow()
            ).getOrThrow()
            conversations.initializeOwnedGroupVerification(groupId).getOrThrow()
            addConversationMembers(conversationId = groupId, peerIds = contactIds).getOrThrow()
            groupId
        }
}
