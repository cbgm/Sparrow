package com.cbgm.sparrow.feature.conversationorchestration.message.data.repository

import com.cbgm.sparrow.core.result.safeSuspendCall
import com.cbgm.sparrow.data.database.entity.ConversationType
import com.cbgm.sparrow.feature.conversationorchestration.message.data.datasource.ConversationDataSource
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.model.DirectConversationTarget
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.repository.DirectConversationRepository

internal class DirectConversationRepositoryImpl(
    private val dataSource: ConversationDataSource
) : DirectConversationRepository {
    override suspend fun getOrCreate(contactId: String): Result<String> =
        safeSuspendCall { dataSource.getOrCreateDirect(contactId).id }

    override suspend fun findConversationId(contactId: String): Result<String?> =
        safeSuspendCall {
            dataSource.findByContactId(contactId)
                ?.takeIf { conversation -> conversation.type == ConversationType.DIRECT.name }
                ?.id
        }

    override suspend fun findContactId(conversationId: String): Result<String?> =
        safeSuspendCall {
            val conversation = dataSource.findById(conversationId) ?: return@safeSuspendCall null
            check(conversation.type == ConversationType.DIRECT.name) { "Conversation is not direct" }
            requireNotNull(conversation.contactId) { "Direct conversation has no contact" }
        }

    override suspend fun requireTarget(conversationId: String): Result<DirectConversationTarget> =
        safeSuspendCall {
            val conversation = dataSource.findById(conversationId) ?: error("Direct conversation was not found")
            check(conversation.type == ConversationType.DIRECT.name) { "Conversation is not direct" }
            val contactId = requireNotNull(conversation.contactId) { "Direct conversation has no contact" }
            DirectConversationTarget(conversationId = conversation.id, contactId = contactId)
        }

    override suspend fun delete(conversationId: String): Result<Unit> =
        safeSuspendCall {
            val conversation = dataSource.findById(conversationId) ?: return@safeSuspendCall
            check(conversation.type == ConversationType.DIRECT.name) { "Conversation is not direct" }
            dataSource.delete(conversationId)
        }
}
