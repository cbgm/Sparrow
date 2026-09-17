package com.cbgm.sparrow.feature.chats.data.orchestration

import com.cbgm.sparrow.feature.chats.data.direct.outgoing.DirectPendingAuthorizationMessageCoordinator
import com.cbgm.sparrow.feature.chats.domain.repository.direct.DirectConversationRepository
import com.cbgm.sparrow.feature.chats.domain.usecase.DeleteConversationUseCase
import com.cbgm.sparrow.feature.chats.domain.usecase.direct.ActivateAuthorizedDirectConversationUseCase
import com.cbgm.sparrow.feature.chats.domain.usecase.direct.DiscardPendingAuthorizationMessagesUseCase
import com.cbgm.sparrow.feature.chats.domain.usecase.direct.GetOrCreateDirectConversationUseCase
import com.cbgm.sparrow.feature.conversationorchestration.domain.port.ConversationPort

internal class ChatsConversationPort(
    private val getOrCreateDirectConversationUseCase: GetOrCreateDirectConversationUseCase,
    private val activateAuthorizedDirectConversationUseCase: ActivateAuthorizedDirectConversationUseCase,
    private val discardPendingAuthorizationMessagesUseCase: DiscardPendingAuthorizationMessagesUseCase,
    private val deleteConversationUseCase: DeleteConversationUseCase,
    private val pendingAuthorizationMessageCoordinator: DirectPendingAuthorizationMessageCoordinator,
    private val conversationRepository: DirectConversationRepository
) : ConversationPort {
    override suspend fun getOrCreateConversation(peerId: String): Result<String> =
        getOrCreateDirectConversationUseCase(peerId)

    override suspend fun activateAuthorizedConversation(peerId: String): Result<Unit> =
        activateAuthorizedDirectConversationUseCase(peerId)

    override suspend fun discardPendingAuthorizationMessages(peerId: String): Result<Unit> =
        discardPendingAuthorizationMessagesUseCase(peerId)

    override suspend fun runPendingAuthorizationCleanup() {
        pendingAuthorizationMessageCoordinator.run()
    }

    override suspend fun findPeerId(conversationId: String): Result<String?> =
        conversationRepository.findContactId(conversationId)

    override suspend fun deleteConversation(conversationId: String): Result<Unit> =
        deleteConversationUseCase(conversationId)
}
