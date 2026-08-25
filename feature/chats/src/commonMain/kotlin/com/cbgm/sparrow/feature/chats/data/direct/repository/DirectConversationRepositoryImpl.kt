package com.cbgm.sparrow.feature.chats.data.direct.repository

import com.cbgm.sparrow.data.database.dao.ChatDao
import com.cbgm.sparrow.data.database.dao.MessageAttachmentDao
import com.cbgm.sparrow.data.database.model.ConversationWithMessages
import com.cbgm.sparrow.feature.attachments.data.datasource.MessageAttachmentDataSource
import com.cbgm.sparrow.feature.attachments.data.mapper.toDomainAttachmentsByMessageId
import com.cbgm.sparrow.feature.attachments.domain.model.MessageMediaAttachment
import com.cbgm.sparrow.feature.chats.data.direct.datasource.DirectConversationDataSource
import com.cbgm.sparrow.feature.chats.data.direct.mapper.toDirectConversation
import com.cbgm.sparrow.feature.chats.domain.model.direct.DirectConversation
import com.cbgm.sparrow.feature.chats.domain.repository.direct.DirectConversationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class DirectConversationRepositoryImpl(
    private val chatDao: ChatDao,
    private val messageAttachmentDao: MessageAttachmentDao,
    private val messageAttachmentTransfer: MessageAttachmentDataSource,
    private val conversationDataSource: DirectConversationDataSource
) : DirectConversationRepository {
    override fun observe(conversationId: String): Flow<DirectConversation?> =
        combine(
            chatDao.observeConversationById(conversationId),
            chatDao.observeRecentMessages(conversationId, RECENT_MESSAGE_LIMIT),
            messageAttachmentDao.observeRecentByConversation(conversationId, RECENT_MESSAGE_LIMIT)
        ) { conversation, recentMessages, attachments ->
            conversation?.let {
                DirectConversationSnapshot(
                    conversation = ConversationWithMessages(it, recentMessages),
                    attachmentsByMessageId = attachments.toDomainAttachmentsByMessageId()
                )
            }
        }.map { result ->
            result
                ?.takeIf { it.conversation.conversation.type == DIRECT_CONVERSATION_TYPE }
                ?.let { snapshot ->
                    snapshot.conversation.toDirectConversation(snapshot.attachmentsByMessageId)
                }
        }

    override suspend fun getOrCreate(contactId: String): String =
        conversationDataSource.getOrCreate(contactId).id

    override suspend fun findContactId(conversationId: String): Result<String?> =
        runCatching {
            val conversation = chatDao.findConversationById(conversationId) ?: return@runCatching null
            check(conversation.type == DIRECT_CONVERSATION_TYPE) { "Conversation is not direct" }
            requireNotNull(conversation.contactId) { "Direct conversation has no contact" }
        }

    override suspend fun delete(conversationId: String): Result<Unit> =
        runCatching {
            val conversation = chatDao.findConversationById(conversationId) ?: return@runCatching
            check(conversation.type == DIRECT_CONVERSATION_TYPE) { "Conversation is not direct" }
            messageAttachmentTransfer.deleteLocalFilesForConversation(conversationId)
            chatDao.deleteConversation(conversationId)
        }

    private data class DirectConversationSnapshot(
        val conversation: ConversationWithMessages,
        val attachmentsByMessageId: Map<String, List<MessageMediaAttachment>>
    )

    private companion object {
        const val DIRECT_CONVERSATION_TYPE = "DIRECT"
        const val RECENT_MESSAGE_LIMIT = 500
    }
}
