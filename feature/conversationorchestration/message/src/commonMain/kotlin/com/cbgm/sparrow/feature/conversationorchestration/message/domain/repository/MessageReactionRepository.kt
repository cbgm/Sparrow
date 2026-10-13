package com.cbgm.sparrow.feature.conversationorchestration.message.domain.repository

/** Reactions are keyed by reactor; a null reactor contact ID is the local user. */
interface MessageReactionRepository {
    suspend fun exists(messageId: String, reactorContactId: String?, emoji: String): Result<Boolean>

    suspend fun save(messageId: String, conversationId: String, reactorContactId: String?, emoji: String): Result<Unit>

    suspend fun delete(messageId: String, reactorContactId: String?, emoji: String): Result<Unit>
}
