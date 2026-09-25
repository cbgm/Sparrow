package com.cbgm.sparrow.feature.chats.data.direct.repository

import com.cbgm.sparrow.core.logging.ChatOpenTrace
import com.cbgm.sparrow.core.result.safeSuspendCall
import com.cbgm.sparrow.data.database.entity.MessageReactionEntity
import com.cbgm.sparrow.data.database.model.ConversationWithMessagesDto
import com.cbgm.sparrow.feature.attachments.domain.repository.MessageAttachmentOperationsRepository
import com.cbgm.sparrow.feature.chats.data.direct.datasource.DirectConversationDataSource
import com.cbgm.sparrow.feature.chats.data.direct.mapper.toDirectConversation
import com.cbgm.sparrow.feature.chats.data.mapper.toMessagePartDtos
import com.cbgm.sparrow.feature.chats.data.model.MessagePartDto
import com.cbgm.sparrow.feature.chats.domain.model.MessageHistoryCursor
import com.cbgm.sparrow.feature.chats.domain.model.MessageHistoryPolicy
import com.cbgm.sparrow.feature.chats.domain.model.MessageReaction
import com.cbgm.sparrow.feature.chats.domain.model.direct.DirectConversation
import com.cbgm.sparrow.feature.chats.domain.repository.direct.DirectConversationRepository
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.shareIn
import kotlin.time.TimeSource

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class DirectConversationRepositoryImpl(
    private val messageAttachmentDataSource: MessageAttachmentOperationsRepository,
    private val conversationDataSource: DirectConversationDataSource
) : DirectConversationRepository {
    override fun observe(
        conversationId: String,
        oldestCursor: MessageHistoryCursor?
    ): Flow<DirectConversation?> = flow {
        coroutineScope {
            val messages =
                oldestCursor?.let { cursor ->
                    conversationDataSource.observeMessagesFromCursor(
                        conversationId = conversationId,
                        fromTimestamp = cursor.createdAtEpochMilliseconds,
                        fromMessageId = cursor.messageId
                    )
                } ?: conversationDataSource.observeRecentMessages(conversationId, MessageHistoryPolicy.PAGE_SIZE)
            val tracedMessages = messages.onEach { loaded ->
                ChatOpenTrace.event("direct Room messages emitted count=${loaded.size}")
            }

            val sharedMessages = tracedMessages.shareIn(
                scope = this,
                started = SharingStarted.Eagerly,
                replay = 1
            )

            val attachments = sharedMessages.map { loaded -> loaded.map { it.id } }.distinctUntilChanged().flatMapLatest { messageIds ->
                messageAttachmentDataSource.observeByMessageIds(messageIds)
            }
            val reactions = observeReactions(conversationId, oldestCursor)

            combine(
                conversationDataSource.observeConversationById(conversationId),
                sharedMessages,
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
            }.onEach { result ->
                ChatOpenTrace.event("direct repo snapshot ready count=${result?.conversation?.messages?.size ?: 0}")
            }.map { result ->
                val mappingStarted = TimeSource.Monotonic.markNow()
                val mapped = result
                    ?.takeIf { it.conversation.conversation.type == DIRECT_CONVERSATION_TYPE }
                    ?.let { snapshot ->
                        snapshot.conversation.toDirectConversation(
                            partsByMessageId = snapshot.partsByMessageId,
                            reactionsByMessageId = snapshot.reactionsByMessageId
                        )
                    }
                ChatOpenTrace.event("direct repo domain mapping completed duration=${mappingStarted.elapsedNow().inWholeMilliseconds}ms messages=${mapped?.messages?.size ?: 0}")
                mapped
            }.onStart {
                ChatOpenTrace.event("direct repo subscribed")
            }.collect { emit(it) }
        }
    }

    override suspend fun getOrCreate(contactId: String): Result<String> = safeSuspendCall {
        conversationDataSource.getOrCreate(contactId).id
    }

    override suspend fun findContactId(conversationId: String): Result<String?> =
        safeSuspendCall {
            val conversation =
                conversationDataSource.findConversationById(conversationId) ?: return@safeSuspendCall null
            check(conversation.type == DIRECT_CONVERSATION_TYPE) { "Conversation is not direct" }
            requireNotNull(conversation.contactId) { "Direct conversation has no contact" }
        }

    override suspend fun findConversationId(contactId: String): Result<String?> =
        safeSuspendCall {
            conversationDataSource
                .findConversationByContactId(contactId)
                ?.takeIf { conversation -> conversation.type == DIRECT_CONVERSATION_TYPE }
                ?.id
        }

    override suspend fun delete(conversationId: String): Result<Unit> =
        safeSuspendCall {
            val conversation =
                conversationDataSource.findConversationById(conversationId) ?: return@safeSuspendCall
            check(conversation.type == DIRECT_CONVERSATION_TYPE) { "Conversation is not direct" }
            conversationDataSource.deleteConversation(conversationId)
        }

    private fun observeReactions(
        conversationId: String,
        oldestCursor: MessageHistoryCursor?
    ): Flow<List<MessageReactionEntity>> =
        oldestCursor?.let { cursor ->
            conversationDataSource.observeReactionsFromCursor(
                conversationId = conversationId,
                fromTimestamp = cursor.createdAtEpochMilliseconds,
                fromMessageId = cursor.messageId
            )
        } ?: conversationDataSource.observeRecentReactions(
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
