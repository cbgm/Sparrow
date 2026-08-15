package com.cbgm.sparrow.feature.chats.data.direct.repository

import com.cbgm.sparrow.core.protocol.mailbox.MailboxCapabilityLifecycle
import com.cbgm.sparrow.data.database.dao.ChatDao
import com.cbgm.sparrow.data.database.model.ConversationWithMessages
import com.cbgm.sparrow.feature.chats.data.direct.mapper.toDirectConversation
import com.cbgm.sparrow.feature.chats.data.direct.storage.DirectConversationStorage
import com.cbgm.sparrow.feature.chats.domain.model.direct.DirectConversation
import com.cbgm.sparrow.feature.chats.domain.repository.direct.DirectConversationRepository
import com.cbgm.sparrow.feature.contacts.domain.repository.IdentityInvitationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class DirectConversationRepositoryImpl(
    private val chatDao: ChatDao,
    private val conversationStorage: DirectConversationStorage,
    private val identityInvitationRepository: IdentityInvitationRepository,
    private val mailboxCapabilityLifecycle: MailboxCapabilityLifecycle
) : DirectConversationRepository {
    override fun observe(conversationId: String): Flow<DirectConversation?> =
        combine(
            chatDao.observeConversationById(conversationId),
            chatDao.observeRecentMessages(conversationId, RECENT_MESSAGE_LIMIT)
        ) { conversation, recentMessages ->
            conversation?.let { ConversationWithMessages(it, recentMessages) }
        }.map { result ->
            result
                ?.takeIf { it.conversation.type == DIRECT_CONVERSATION_TYPE }
                ?.toDirectConversation()
        }

    override suspend fun getOrCreate(contactId: String): String =
        conversationStorage.getOrCreate(contactId).id

    override suspend fun delete(conversationId: String): Result<Unit> =
        runCatching {
            val conversation =
                chatDao.findConversationById(conversationId)
                    ?: return@runCatching
            check(conversation.type == DIRECT_CONVERSATION_TYPE) {
                "Conversation is not direct"
            }

            val contactId =
                requireNotNull(conversation.contactId) {
                    "Direct conversation has no contact"
                }
            identityInvitationRepository.revokeDirectChatAuthorization(contactId).getOrThrow()
            mailboxCapabilityLifecycle.revokeForContact(contactId).getOrThrow()
            chatDao.deleteConversation(conversationId)
        }

    private companion object {
        const val DIRECT_CONVERSATION_TYPE = "DIRECT"
        const val RECENT_MESSAGE_LIMIT = 500
    }
}
