package com.cbgm.securechat.feature.chats.data.direct.repository

import com.cbgm.securechat.core.protocol.mailbox.MailboxCapabilityLifecycle
import com.cbgm.securechat.data.database.dao.ChatDao
import com.cbgm.securechat.feature.chats.data.direct.mapper.toDirectConversation
import com.cbgm.securechat.feature.chats.data.direct.storage.DirectConversationStorage
import com.cbgm.securechat.feature.chats.domain.model.direct.DirectConversation
import com.cbgm.securechat.feature.chats.domain.repository.direct.DirectConversationRepository
import com.cbgm.securechat.feature.contacts.domain.identity.IdentityInvitationService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class DirectConversationRepositoryImpl(
    private val chatDao: ChatDao,
    private val conversationStorage: DirectConversationStorage,
    private val identityInvitationService: IdentityInvitationService,
    private val mailboxCapabilityLifecycle: MailboxCapabilityLifecycle
) : DirectConversationRepository {
    override fun observe(conversationId: String): Flow<DirectConversation?> =
        chatDao.observeConversationWithMessagesById(conversationId).map { result ->
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
            identityInvitationService.revokeDirectChatAuthorization(contactId).getOrThrow()
            mailboxCapabilityLifecycle.revokeForContact(contactId).getOrThrow()
            chatDao.deleteConversation(conversationId)
        }

    private companion object {
        const val DIRECT_CONVERSATION_TYPE = "DIRECT"
    }
}
