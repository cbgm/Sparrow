package com.cbgm.sparrow.feature.chats.data.group.repository

import com.cbgm.sparrow.data.database.dao.ChatDao
import com.cbgm.sparrow.data.database.dao.GroupInvitationDao
import com.cbgm.sparrow.data.database.dao.GroupSecurityDao
import com.cbgm.sparrow.data.database.dao.GroupVerificationDao
import com.cbgm.sparrow.data.database.dao.MessageReactionDao
import com.cbgm.sparrow.data.database.dao.MessageRecipientStateDao
import com.cbgm.sparrow.data.database.entity.ConversationEntity
import com.cbgm.sparrow.data.database.entity.GroupInvitationEntity
import com.cbgm.sparrow.data.database.entity.GroupVerificationPairEntity
import com.cbgm.sparrow.data.database.entity.MessageEntity
import com.cbgm.sparrow.data.database.entity.MessageReactionEntity
import com.cbgm.sparrow.data.database.entity.MessageRecipientStateEntity
import com.cbgm.sparrow.data.database.model.ConversationWithMessagesDto
import com.cbgm.sparrow.feature.attachments.data.datasource.MessageAttachmentDataSource
import com.cbgm.sparrow.feature.attachments.domain.model.MessageAttachment
import com.cbgm.sparrow.feature.chats.data.group.mapper.GroupMembershipMessageFactory
import com.cbgm.sparrow.feature.chats.data.group.mapper.toGroupConversation
import com.cbgm.sparrow.feature.chats.data.mapper.toMessagePartDtos
import com.cbgm.sparrow.feature.chats.data.model.MessagePartDto
import com.cbgm.sparrow.feature.chats.domain.model.MessageHistoryCursor
import com.cbgm.sparrow.feature.chats.domain.model.MessageHistoryPolicy
import com.cbgm.sparrow.feature.chats.domain.model.MessageReaction
import com.cbgm.sparrow.feature.chats.domain.model.group.GroupConversation
import com.cbgm.sparrow.feature.chats.domain.repository.group.GroupConversationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class GroupConversationRepositoryImpl(
    private val chatDao: ChatDao,
    private val messageAttachmentDataSource: MessageAttachmentDataSource,
    private val messageRecipientStateDao: MessageRecipientStateDao,
    private val messageReactionDao: MessageReactionDao,
    private val groupInvitationDao: GroupInvitationDao,
    private val groupSecurityDao: GroupSecurityDao,
    private val groupVerificationDao: GroupVerificationDao
) : GroupConversationRepository {
    override fun observe(
        groupId: String,
        oldestCursor: MessageHistoryCursor?
    ): Flow<GroupConversation?> {
        val messages =
            oldestCursor?.let { cursor ->
                chatDao.observeMessagesFromCursor(
                    conversationId = groupId,
                    fromTimestamp = cursor.createdAtEpochMilliseconds,
                    fromMessageId = cursor.messageId
                )
            } ?: chatDao.observeRecentMessages(groupId, MessageHistoryPolicy.PAGE_SIZE)

        val messageSnapshot =
            combine(
                chatDao.observeConversationById(groupId),
                messages,
                observeAttachments(groupId, oldestCursor),
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
                groupSecurityDao.observeCurrentMemberKeys(groupId),
                observeRecipientStates(groupId, oldestCursor),
                groupInvitationDao.observeByGroupId(groupId),
                groupVerificationDao.observeByGroupId(groupId)
            ) { memberKeys, recipientStates, invitations, verificationRows ->
                GroupStateSnapshotDto(
                    participantContactIds = memberKeys.map { memberKey -> memberKey.contactId },
                    recipientStates = recipientStates,
                    invitations = invitations,
                    verificationRows = verificationRows
                )
            }

        return combine(
            messageSnapshot,
            groupStateSnapshot,
            chatDao.observeMessagesByTransportModes(groupId, LOCAL_MEMBERSHIP_TRANSPORT_MODES)
        ) { snapshot, groupState, membershipHistory ->
            snapshot.conversation
                ?.takeIf { it.type == GROUP_CONVERSATION_TYPE }
                ?.let { ConversationWithMessagesDto(it, snapshot.messages) }
                ?.toGroupConversation(
                    participantContactIds = groupState.participantContactIds,
                    recipientStates = groupState.recipientStates,
                    invitations = groupState.invitations,
                    verificationRows = groupState.verificationRows,
                    partsByMessageId = snapshot.partsByMessageId,
                    reactionsByMessageId = snapshot.reactionsByMessageId,
                    localMembershipHistory = membershipHistory
                )
        }
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

    private fun observeRecipientStates(
        conversationId: String,
        oldestCursor: MessageHistoryCursor?
    ): Flow<List<MessageRecipientStateEntity>> =
        oldestCursor?.let { cursor ->
            messageRecipientStateDao.observeFromMessageCursor(
                conversationId = conversationId,
                fromTimestamp = cursor.createdAtEpochMilliseconds,
                fromMessageId = cursor.messageId
            )
        } ?: messageRecipientStateDao.observeRecentByConversationId(
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
        val invitations: List<GroupInvitationEntity>,
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
