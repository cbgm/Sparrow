package com.cbgm.sparrow.feature.chats.data.group.repository

import com.cbgm.sparrow.core.id.IdGenerator
import com.cbgm.sparrow.core.time.SystemClock
import com.cbgm.sparrow.data.database.entity.ConversationEntity
import com.cbgm.sparrow.data.database.entity.GroupVerificationPairEntity
import com.cbgm.sparrow.data.database.entity.MessageEntity
import com.cbgm.sparrow.data.database.entity.MessageReactionEntity
import com.cbgm.sparrow.data.database.entity.MessageRecipientStateEntity
import com.cbgm.sparrow.data.database.model.ConversationWithMessagesDto
import com.cbgm.sparrow.feature.attachments.domain.repository.MessageAttachmentOperationsRepository
import com.cbgm.sparrow.feature.chats.data.group.datasource.GroupConversationHistoryDataSource
import com.cbgm.sparrow.feature.chats.data.group.mapper.GroupMembershipMessageFactory
import com.cbgm.sparrow.feature.chats.data.group.mapper.toGroupConversation
import com.cbgm.sparrow.feature.chats.data.mapper.toMessagePartDtos
import com.cbgm.sparrow.feature.chats.data.model.MessagePartDto
import com.cbgm.sparrow.feature.chats.domain.model.MessageHistoryCursor
import com.cbgm.sparrow.feature.chats.domain.model.MessageHistoryPolicy
import com.cbgm.sparrow.feature.chats.domain.model.MessageReaction
import com.cbgm.sparrow.feature.chats.domain.model.group.GroupConversation
import com.cbgm.sparrow.feature.chats.domain.repository.group.GroupConversationRepository
import com.cbgm.sparrow.feature.membership.domain.model.GroupMemberLifecycleSnapshot
import com.cbgm.sparrow.feature.membership.domain.repository.GroupMembershipRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
internal class GroupConversationRepositoryImpl(
    private val historyDataSource: GroupConversationHistoryDataSource,
    private val messageAttachmentDataSource: MessageAttachmentOperationsRepository,
    private val membershipRepository: GroupMembershipRepository
) : GroupConversationRepository {
    override suspend fun create(title: String): Result<String> =
        runCatching {
            val normalizedTitle = title.trim()
            require(normalizedTitle.isNotEmpty()) { "Group title must not be blank" }
            val now = SystemClock.nowEpochMilliseconds()
            val groupId = IdGenerator.generate(prefix = "group")
            historyDataSource.upsertConversation(
                ConversationEntity(
                    id = groupId,
                    contactId = null,
                    type = GROUP_CONVERSATION_TYPE,
                    title = normalizedTitle,
                    createdAtEpochMilliseconds = now,
                    updatedAtEpochMilliseconds = now
                )
            )
            groupId
        }

    override fun observe(
        groupId: String,
        oldestCursor: MessageHistoryCursor?
    ): Flow<GroupConversation?> {
        val messages =
            oldestCursor?.let { cursor ->
                historyDataSource.observeMessagesFromCursor(
                    conversationId = groupId,
                    fromTimestamp = cursor.createdAtEpochMilliseconds,
                    fromMessageId = cursor.messageId
                )
            } ?: historyDataSource.observeRecentMessages(groupId, MessageHistoryPolicy.PAGE_SIZE)

        val messageSnapshot =
            combine(
                historyDataSource.observeConversation(groupId),
                messages,
                messages.map { loaded -> loaded.map { it.id } }.distinctUntilChanged().flatMapLatest { messageIds ->
                    messageAttachmentDataSource.observeByMessageIds(messageIds)
                },
                observeReactions(groupId, oldestCursor)
            ) { conversation, loadedMessages, attachmentsByMessageId, reactions ->
                MessageSnapshotDto(
                    conversation = conversation,
                    messages = loadedMessages,
                    partsByMessageId =
                        attachmentsByMessageId.mapValues { (_, values) ->
                            values.toMessagePartDtos()
                        },
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

        return combine(
            messageSnapshot,
            groupStateSnapshot,
            historyDataSource.observeMessagesByTransportModes(groupId, LOCAL_MEMBERSHIP_TRANSPORT_MODES)
        ) { snapshot, groupState, membershipHistory ->
            snapshot.conversation
                ?.takeIf { it.type == GROUP_CONVERSATION_TYPE }
                ?.let { ConversationWithMessagesDto(it, snapshot.messages) }
                ?.toGroupConversation(
                    participantContactIds = groupState.participantContactIds,
                    recipientStates = groupState.recipientStates,
                    memberships = groupState.memberships,
                    verificationRows = groupState.verificationRows,
                    partsByMessageId = snapshot.partsByMessageId,
                    reactionsByMessageId = snapshot.reactionsByMessageId,
                    localMembershipHistory = membershipHistory
                )
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
        val partsByMessageId: Map<String, List<MessagePartDto>>,
        val reactionsByMessageId: Map<String, List<MessageReaction>>
    )

    private companion object {
        const val GROUP_CONVERSATION_TYPE = "GROUP"

        val LOCAL_MEMBERSHIP_TRANSPORT_MODES =
            listOf(
                GroupMembershipMessageFactory.LOCAL_MEMBERSHIP_STARTED_TRANSPORT_MODE,
                GroupMembershipMessageFactory.LOCAL_MEMBERSHIP_REMOVED_TRANSPORT_MODE,
                GroupMembershipMessageFactory.LOCAL_MEMBERSHIP_LEFT_TRANSPORT_MODE
            )
    }
}
