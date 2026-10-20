package com.cbgm.sparrow.feature.conversationorchestration.message.data.repository

import com.cbgm.sparrow.core.util.result.safeSuspendCall
import com.cbgm.sparrow.data.database.entity.MessageReactionEntity
import com.cbgm.sparrow.feature.conversationorchestration.message.data.datasource.MessageReactionDataSource
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.repository.MessageReactionRepository

internal class MessageReactionRepositoryImpl(
    private val dataSource: MessageReactionDataSource
) : MessageReactionRepository {
    override suspend fun exists(messageId: String, reactorContactId: String?, emoji: String): Result<Boolean> =
        safeSuspendCall { dataSource.find(messageId, reactorContactId.toReactorId(), emoji) != null }

    override suspend fun save(
        messageId: String,
        conversationId: String,
        reactorContactId: String?,
        emoji: String
    ): Result<Unit> =
        safeSuspendCall {
            dataSource.upsert(MessageReactionEntity(messageId, conversationId, reactorContactId.toReactorId(), emoji))
        }

    override suspend fun delete(messageId: String, reactorContactId: String?, emoji: String): Result<Unit> =
        safeSuspendCall { dataSource.delete(messageId, reactorContactId.toReactorId(), emoji) }

    private fun String?.toReactorId(): String = this ?: MessageReactionEntity.LOCAL_REACTOR_ID
}
