package com.cbgm.sparrow.feature.chats.data.orchestration

import com.cbgm.sparrow.feature.chats.data.direct.outgoing.DirectPendingAuthorizationMessageCoordinator
import com.cbgm.sparrow.feature.chats.domain.repository.direct.DirectConversationRepository
import com.cbgm.sparrow.feature.chats.domain.repository.direct.DirectMessageRepository
import com.cbgm.sparrow.feature.conversationorchestration.domain.port.ConversationPort

internal class ChatsConversationPort(
    private val conversationRepository: DirectConversationRepository,
    private val directMessageRepository: DirectMessageRepository,
    private val pendingAuthorizationMessageCoordinator: DirectPendingAuthorizationMessageCoordinator
) : ConversationPort {
    override suspend fun getOrCreateConversation(peerId: String): Result<String> =
        conversationRepository.getOrCreate(peerId)

    override suspend fun activateAuthorizedConversation(peerId: String): Result<Unit> =
        directMessageRepository.releaseWaitingForAuthorization(peerId)

    override suspend fun discardPendingAuthorizationMessages(peerId: String): Result<Unit> =
        directMessageRepository.discardWaitingForAuthorization(peerId)

    override suspend fun runPendingAuthorizationCleanup() {
        pendingAuthorizationMessageCoordinator.run()
    }

    override suspend fun findPeerId(conversationId: String): Result<String?> =
        conversationRepository.findContactId(conversationId)

    override suspend fun deleteConversation(conversationId: String): Result<Unit> =
        conversationRepository.delete(conversationId)
}
