package com.cbgm.sparrow.feature.chats.data.orchestration

import com.cbgm.sparrow.feature.chats.data.direct.outgoing.DirectPendingAuthorizationMessageCoordinator
import com.cbgm.sparrow.feature.chats.domain.repository.direct.DirectConversationRepository
import com.cbgm.sparrow.feature.chats.domain.usecase.direct.ActivateAuthorizedDirectConversationUseCase
import com.cbgm.sparrow.feature.chats.domain.usecase.direct.DeleteDirectConversationUseCase
import com.cbgm.sparrow.feature.chats.domain.usecase.direct.DiscardPendingAuthorizationMessagesUseCase
import com.cbgm.sparrow.feature.conversationorchestration.domain.port.DirectConversationPort

internal class ChatsDirectConversationPort(
    private val activateAuthorizedDirectConversationUseCase: ActivateAuthorizedDirectConversationUseCase,
    private val discardPendingAuthorizationMessagesUseCase: DiscardPendingAuthorizationMessagesUseCase,
    private val deleteDirectConversationUseCase: DeleteDirectConversationUseCase,
    private val pendingAuthorizationMessageCoordinator: DirectPendingAuthorizationMessageCoordinator,
    private val conversationRepository: DirectConversationRepository
) : DirectConversationPort {
    override suspend fun activateAuthorizedConversation(contactId: String): Result<Unit> =
        activateAuthorizedDirectConversationUseCase(contactId)

    override suspend fun discardPendingAuthorizationMessages(contactId: String): Result<Unit> =
        discardPendingAuthorizationMessagesUseCase(contactId)

    override suspend fun runPendingAuthorizationCleanup() {
        pendingAuthorizationMessageCoordinator.run()
    }

    override suspend fun findContactId(conversationId: String): Result<String?> =
        conversationRepository.findContactId(conversationId)

    override suspend fun deleteConversation(conversationId: String): Result<Unit> =
        deleteDirectConversationUseCase(conversationId)
}
