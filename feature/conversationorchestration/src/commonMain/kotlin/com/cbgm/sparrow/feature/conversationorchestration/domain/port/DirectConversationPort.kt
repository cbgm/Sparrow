package com.cbgm.sparrow.feature.conversationorchestration.domain.port

interface DirectConversationPort {
    suspend fun activateAuthorizedConversation(contactId: String): Result<Unit>

    suspend fun discardPendingAuthorizationMessages(contactId: String): Result<Unit>

    suspend fun runPendingAuthorizationCleanup()

    suspend fun findContactId(conversationId: String): Result<String?>

    suspend fun deleteConversation(conversationId: String): Result<Unit>
}
