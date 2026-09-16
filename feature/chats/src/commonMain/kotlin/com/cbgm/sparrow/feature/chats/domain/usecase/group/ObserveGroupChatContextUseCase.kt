package com.cbgm.sparrow.feature.chats.domain.usecase.group

import com.cbgm.sparrow.feature.chats.domain.model.MessageHistoryCursor
import com.cbgm.sparrow.feature.chats.domain.model.group.GroupChatContext
import com.cbgm.sparrow.feature.chats.domain.model.group.GroupConversation
import com.cbgm.sparrow.feature.chats.domain.model.group.GroupPin
import com.cbgm.sparrow.feature.chats.domain.repository.group.GroupConversationRepository
import com.cbgm.sparrow.feature.chats.domain.repository.group.GroupPinRepository
import com.cbgm.sparrow.feature.contacts.domain.model.Contact
import com.cbgm.sparrow.feature.contacts.domain.repository.ContactRepository
import com.cbgm.sparrow.feature.membership.domain.model.GroupAdministrationState
import com.cbgm.sparrow.feature.membership.domain.repository.GroupMembershipRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart

class ObserveGroupChatContextUseCase(
    private val conversationRepository: GroupConversationRepository,
    private val membershipRepository: GroupMembershipRepository,
    private val contactRepository: ContactRepository,
    private val pinRepository: GroupPinRepository
) {
    operator fun invoke(
        groupId: String,
        oldestCursor: MessageHistoryCursor? = null
    ): Flow<GroupChatContext> {
        val conversationFlow =
            conversationRepository
                .observe(groupId, oldestCursor)
                .map { conversation -> ConversationSnapshot(conversation = conversation) }
                .catch { error -> emit(ConversationSnapshot(conversation = null, error = error)) }

        val contactsFlow: Flow<List<Contact>> =
            contactRepository
                .observeContacts()
                .onStart { emit(emptyList()) }
                .catch { emit(emptyList()) }

        val pinFlow: Flow<GroupPin?> =
            pinRepository
                .observe(groupId)
                .onStart { emit(null) }
                .catch { emit(null) }

        val metadataFlow =
            combine(
                membershipRepository
                    .observeAdministration(groupId)
                    .onStart { emit(GroupAdministrationState()) },
                contactsFlow,
                pinFlow
            ) { administration, contacts, pin ->
                GroupChatMetadata(
                    administration = administration,
                    contacts = contacts,
                    pin = pin
                )
            }

        return combine(conversationFlow, metadataFlow) { conversation, metadata ->
            GroupChatContext(
                conversation = conversation.conversation,
                conversationError = conversation.error,
                administration = metadata.administration,
                contacts = metadata.contacts,
                pin = metadata.pin
            )
        }
    }

    private data class GroupChatMetadata(
        val administration: GroupAdministrationState,
        val contacts: List<Contact>,
        val pin: GroupPin?
    )

    private data class ConversationSnapshot(
        val conversation: GroupConversation?,
        val error: Throwable? = null
    )
}
