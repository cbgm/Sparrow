package com.cbgm.sparrow.feature.chats.data.group.repository

import com.cbgm.sparrow.core.logging.ChatOpenTrace
import com.cbgm.sparrow.core.message.domain.model.MessageTransportMode
import com.cbgm.sparrow.core.messagepart.data.mapper.toMessagePart
import com.cbgm.sparrow.core.messagepart.domain.model.MessagePart
import com.cbgm.sparrow.data.database.entity.ConversationEntity
import com.cbgm.sparrow.data.database.entity.GroupVerificationPairEntity
import com.cbgm.sparrow.data.database.entity.MessageEntity
import com.cbgm.sparrow.data.database.entity.MessageReactionEntity
import com.cbgm.sparrow.data.database.entity.MessageRecipientStateEntity
import com.cbgm.sparrow.data.database.model.ConversationWithMessagesDto
import com.cbgm.sparrow.feature.attachments.domain.repository.MessageAttachmentOperationsRepository
import com.cbgm.sparrow.feature.chats.data.group.datasource.GroupConversationHistoryDataSource
import com.cbgm.sparrow.feature.chats.data.group.mapper.toGroupConversation
import com.cbgm.sparrow.feature.chats.domain.model.MessageHistoryCursor
import com.cbgm.sparrow.feature.chats.domain.model.MessageHistoryPolicy
import com.cbgm.sparrow.feature.chats.domain.model.MessageReaction
import com.cbgm.sparrow.feature.chats.domain.model.group.GroupConversation
import com.cbgm.sparrow.feature.chats.domain.repository.group.GroupConversationReadRepository
import com.cbgm.sparrow.feature.membership.domain.model.GroupMemberLifecycleSnapshot
import com.cbgm.sparrow.feature.membership.domain.repository.GroupMembershipRepository
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
internal class GroupConversationReadRepositoryImpl(
    private val historyDataSource: GroupConversationHistoryDataSource,
    private val messageAttachmentDataSource: MessageAttachmentOperationsRepository,
    private val membershipRepository: GroupMembershipRepository
) : GroupConversationReadRepository {
    override fun observe(
        groupId: String,
        oldestCursor: MessageHistoryCursor?
    ): Flow<GroupConversation?> = flow {
        coroutineScope {
            val messages =
                oldestCursor?.let { cursor ->
                    historyDataSource.observeMessagesFromCursor(
                        conversationId = groupId,
                        fromTimestamp = cursor.createdAtEpochMilliseconds,
                        fromMessageId = cursor.messageId
                    )
                } ?: historyDataSource.observeRecentMessages(groupId, MessageHistoryPolicy.PAGE_SIZE)
            val tracedMessages = messages.onEach { loaded ->
                ChatOpenTrace.event("group Room messages emitted count=${loaded.size}")
            }

            val sharedMessages = tracedMessages.shareIn(
                scope = this,
                started = SharingStarted.Eagerly,
                replay = 1
            )

            val messageIds = sharedMessages.map { loaded -> loaded.map { it.id } }.distinctUntilChanged()
            val messageSnapshot =
                combine(
                    historyDataSource.observeConversation(groupId),
                    sharedMessages,
                    messageIds.flatMapLatest { ids ->
                        messageAttachmentDataSource.observeByMessageIds(ids)
                    },
                    messageIds.flatMapLatest { ids ->
                        historyDataSource.observeTextPartsByMessageIds(ids)
                    },
                    observeReactions(groupId, oldestCursor)
                ) { conversation, loadedMessages, attachmentsByMessageId, loadedTextParts, reactions ->
                    val parts =
                        attachmentsByMessageId
                            .mapValues { (_, values) -> values.toMutableList() }
                            .toMutableMap()
                    loadedTextParts.forEach { (messageId, textParts) ->
                        parts.getOrPut(messageId) { mutableListOf() }
                            .addAll(0, textParts.map { part -> part.toMessagePart() })
                    }
                    MessageSnapshotDto(
                        conversation = conversation,
                        messages = loadedMessages,
                        partsByMessageId = parts,
                        reactionsByMessageId = reactions.toDomainReactionsByMessageId()
                    )
                }

            val groupStateSnapshot =
                combine(
                    membershipRepository.observeConversationMembership(groupId),
                    observeRecipientStates(groupId, oldestCursor),
                    historyDataSource.observeVerificationRows(groupId)
                ) { membership, recipientStates, verificationRows ->
                    GroupStateSnapshotDto(
                        participantContactIds = membership.participantContactIds,
                        recipientStates = recipientStates,
                        memberships = membership.memberships,
                        verificationRows = verificationRows
                    )
                }

            combine(
                messageSnapshot,
                groupStateSnapshot,
                historyDataSource.observeMessagesByTransportModes(groupId, LOCAL_MEMBERSHIP_TRANSPORT_MODES),
                historyDataSource.observeTitle(groupId)
            ) { snapshot, groupState, membershipHistory, title ->
                val mappingStarted = TimeSource.Monotonic.markNow()
                ChatOpenTrace.event("group repo joined snapshot messages=${snapshot.messages.size}")
                val mapped = snapshot.conversation
                    ?.takeIf { it.type == GROUP_CONVERSATION_TYPE }
                    ?.let { ConversationWithMessagesDto(it, snapshot.messages) }
                    ?.toGroupConversation(
                        title = title,
                        participantContactIds = groupState.participantContactIds,
                        recipientStates = groupState.recipientStates,
                        memberships = groupState.memberships,
                        verificationRows = groupState.verificationRows,
                        partsByMessageId = snapshot.partsByMessageId,
                        reactionsByMessageId = snapshot.reactionsByMessageId,
                        localMembershipHistory = membershipHistory
                    )
                ChatOpenTrace.event(
                    "group repo domain mapping completed duration=${mappingStarted.elapsedNow().inWholeMilliseconds}ms messages=${mapped?.messages?.size ?: 0}"
                )
                mapped
            }.onStart {
                ChatOpenTrace.event("group repo subscribed")
            }.collect { emit(it) }
        }
    }

    private fun observeReactions(
        conversationId: String,
        oldestCursor: MessageHistoryCursor?
    ): Flow<List<MessageReactionEntity>> =
        oldestCursor?.let { cursor ->
            historyDataSource.observeReactionsFromCursor(
                conversationId = conversationId,
                fromTimestamp = cursor.createdAtEpochMilliseconds,
                fromMessageId = cursor.messageId
            )
        } ?: historyDataSource.observeRecentReactions(
            conversationId = conversationId,
            messageLimit = MessageHistoryPolicy.PAGE_SIZE
        )

    private fun observeRecipientStates(
        conversationId: String,
        oldestCursor: MessageHistoryCursor?
    ): Flow<List<MessageRecipientStateEntity>> =
        oldestCursor?.let { cursor ->
            historyDataSource.observeRecipientStatesFromCursor(
                conversationId = conversationId,
                fromTimestamp = cursor.createdAtEpochMilliseconds,
                fromMessageId = cursor.messageId
            )
        } ?: historyDataSource.observeRecentRecipientStates(
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

    private data class GroupStateSnapshotDto(
        val participantContactIds: List<String>,
        val recipientStates: List<MessageRecipientStateEntity>,
        val memberships: List<GroupMemberLifecycleSnapshot>,
        val verificationRows: List<GroupVerificationPairEntity>
    )

    private data class MessageSnapshotDto(
        val conversation: ConversationEntity?,
        val messages: List<MessageEntity>,
        val partsByMessageId: Map<String, List<MessagePart>>,
        val reactionsByMessageId: Map<String, List<MessageReaction>>
    )

    private companion object {
        const val GROUP_CONVERSATION_TYPE = "GROUP"

        val LOCAL_MEMBERSHIP_TRANSPORT_MODES =
            listOf(
                MessageTransportMode.LOCAL_GROUP_MEMBERSHIP_STARTED,
                MessageTransportMode.LOCAL_GROUP_MEMBERSHIP_REMOVED,
                MessageTransportMode.LOCAL_GROUP_MEMBERSHIP_LEFT
            )
    }
}
