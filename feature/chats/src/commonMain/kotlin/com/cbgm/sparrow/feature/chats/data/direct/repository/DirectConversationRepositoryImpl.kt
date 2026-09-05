package com.cbgm.sparrow.feature.chats.data.direct.repository

import com.cbgm.sparrow.core.result.safeSuspendCall
import com.cbgm.sparrow.data.database.dao.ChatDao
import com.cbgm.sparrow.data.database.dao.MessageReactionDao
import com.cbgm.sparrow.data.database.entity.MessageReactionEntity
import com.cbgm.sparrow.data.database.model.ConversationWithMessagesDto
import com.cbgm.sparrow.feature.attachments.data.datasource.MessageAttachmentDataSource
import com.cbgm.sparrow.feature.attachments.domain.model.MessageAttachment
import com.cbgm.sparrow.feature.chats.data.direct.datasource.DirectConversationDataSource
import com.cbgm.sparrow.feature.chats.data.direct.mapper.toDirectConversation
import com.cbgm.sparrow.feature.chats.data.mapper.toMessagePartDtos
import com.cbgm.sparrow.feature.chats.data.model.MessagePartDto
import com.cbgm.sparrow.feature.chats.domain.model.MessageHistoryCursor
import com.cbgm.sparrow.feature.chats.domain.model.MessageHistoryPolicy
import com.cbgm.sparrow.feature.chats.domain.model.MessageReaction
import com.cbgm.sparrow.feature.chats.domain.model.direct.DirectConversation
import com.cbgm.sparrow.feature.chats.domain.repository.direct.DirectConversationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class DirectConversationRepositoryImpl(
    private val chatDao: ChatDao,
    private val messageReactionDao: MessageReactionDao,
    private val messageAttachmentDataSource: MessageAttachmentDataSource,
    private val conversationDataSource: DirectConversationDataSource
) : DirectConversationRepository {
    override fun observe(
        conversationId: String,
        oldestCursor: MessageHistoryCursor?
    ): Flow<DirectConversation?> {
        val messages =
            oldestCursor?.let { cursor ->
                chatDao.observeMessagesFromCursor(
                    conversationId = conversationId,
                    fromTimestamp = cursor.createdAtEpochMilliseconds,
                    fromMessageId = cursor.messageId
                )
            } ?: chatDao.observeRecentMessages(conversationId, MessageHistoryPolicy.PAGE_SIZE)

        val attachments = observeAttachments(conversationId, oldestCursor)
        val reactions = observeReactions(conversationId, oldestCursor)

        return combine(
            chatDao.observeConversationById(conversationId),
            messages,
            attachments,
            reactions
        ) { conversation, loadedMessages, attachmentsByMessageId, loadedReactions ->
            conversation?.let {
                DirectConversationSnapshotDto(
                    conversation = ConversationWithMessagesDto(it, loadedMessages),
                    partsByMessageId =
                        attachmentsByMessageId.mapValues { (_, values) ->
                            values.toMessagePartDtos()
                        },
                    reactionsByMessageId = loadedReactions.toDomainReactionsByMessageId()
                )
            }
        }.map { result ->
            result
                ?.takeIf { it.conversation.conversation.type == DIRECT_CONVERSATION_TYPE }
                ?.let { snapshot ->
                    snapshot.conversation.toDirectConversation(
                        partsByMessageId = snapshot.partsByMessageId,
                        reactionsByMessageId = snapshot.reactionsByMessageId
                    )
                }
        }
    }

    override suspend fun getOrCreate(contactId: String): Result<String> = safeSuspendCall {
        conversationDataSource.getOrCreate(contactId).id
    }

    override suspend fun findContactId(conversationId: String): Result<String?> =
        safeSuspendCall {
            val conversation =
                chatDao.findConversationById(conversationId) ?: return@safeSuspendCall null
            check(conversation.type == DIRECT_CONVERSATION_TYPE) { "Conversation is not direct" }
            requireNotNull(conversation.contactId) { "Direct conversation has no contact" }
        }

    override suspend fun findConversationId(contactId: String): Result<String?> =
        safeSuspendCall {
            chatDao
                .findConversationByContactId(contactId)
                ?.takeIf { conversation -> conversation.type == DIRECT_CONVERSATION_TYPE }
                ?.id
        }

    override suspend fun delete(conversationId: String): Result<Unit> =
        safeSuspendCall {
            val conversation =
                chatDao.findConversationById(conversationId) ?: return@safeSuspendCall
            check(conversation.type == DIRECT_CONVERSATION_TYPE) { "Conversation is not direct" }
            chatDao.deleteConversation(conversationId)
        }

    private fun observeAttachments(
        conversationId: String,
        oldestCursor: MessageHistoryCursor?
    ): Flow<Map<String, List<MessageAttachment>>> =
        oldestCursor?.let { cursor ->
            messageAttachmentDataSource.observeFromMessageCursor(
                conversationId = conversationId,
                fromTimestamp = cursor.createdAtEpochMilliseconds,
                fromMessageId = cursor.messageId
            )
        } ?: messageAttachmentDataSource.observeRecentByConversation(
            conversationId = conversationId,
            messageLimit = MessageHistoryPolicy.PAGE_SIZE
        )

    private fun observeReactions(
        conversationId: String,
        oldestCursor: MessageHistoryCursor?
    ): Flow<List<MessageReactionEntity>> =
        oldestCursor?.let { cursor ->
            messageReactionDao.observeFromMessageCursor(
                conversationId = conversationId,
                fromTimestamp = cursor.createdAtEpochMilliseconds,
                fromMessageId = cursor.messageId
            )
        } ?: messageReactionDao.observeRecentByConversationId(
            conversationId = conversationId,
            messageLimit = MessageHistoryPolicy.PAGE_SIZE
        )

    private fun List<MessageReactionEntity>.toDomainReactionsByMessageId(): Map<String, List<MessageReaction>> =
        groupBy(MessageReactionEntity::messageId)
            .mapValues { (_, values) ->
                values.map { reaction ->
                    MessageReaction(
                        emoji = reaction.emoji,
                        isMine = reaction.reactorId == MessageReactionEntity.LOCAL_REACTOR_ID,
                        reactorContactId =
                            reaction.reactorId.takeUnless {
                                it == MessageReactionEntity.LOCAL_REACTOR_ID
                            }
                    )
                }
            }

    private data class DirectConversationSnapshotDto(
        val conversation: ConversationWithMessagesDto,
        val partsByMessageId: Map<String, List<MessagePartDto>>,
        val reactionsByMessageId: Map<String, List<MessageReaction>>
    )

    private companion object {
        const val DIRECT_CONVERSATION_TYPE = "DIRECT"
    }
}
