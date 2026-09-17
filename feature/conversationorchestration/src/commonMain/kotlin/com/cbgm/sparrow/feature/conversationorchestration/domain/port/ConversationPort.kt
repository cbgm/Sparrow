package com.cbgm.sparrow.feature.conversationorchestration.domain.port

interface ConversationPort {
    suspend fun getOrCreateConversation(peerId: String): Result<String>

    suspend fun activateAuthorizedConversation(peerId: String): Result<Unit>

    suspend fun discardPendingAuthorizationMessages(peerId: String): Result<Unit>

    suspend fun runPendingAuthorizationCleanup()

    suspend fun findPeerId(conversationId: String): Result<String?>

    suspend fun deleteConversation(conversationId: String): Result<Unit>
}
