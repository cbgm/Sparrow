package com.cbgm.sparrow.feature.chats.domain.usecase.group

import com.cbgm.sparrow.feature.chats.domain.model.group.GroupAdministrationState
import com.cbgm.sparrow.feature.chats.domain.model.group.GroupChatContext
import com.cbgm.sparrow.feature.chats.domain.model.group.GroupConversation
import com.cbgm.sparrow.feature.chats.domain.usecase.profile.ObserveRemoteProfilePicturesUseCase
import com.cbgm.sparrow.feature.contacts.domain.model.Contact
import com.cbgm.sparrow.feature.contacts.domain.usecase.ObserveContactsUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart

@OptIn(ExperimentalCoroutinesApi::class)
class ObserveGroupChatContextUseCase(
    private val observeConversation: ObserveGroupConversationUseCase,
    private val observeAdministration: ObserveGroupAdministrationUseCase,
    private val observeContacts: ObserveContactsUseCase,
    private val observeProfilePictures: ObserveRemoteProfilePicturesUseCase,
    private val observeGroupAvatar: ObserveGroupAvatarUseCase
) {
    operator fun invoke(groupId: String): Flow<GroupChatContext> {
        val conversationFlow =
            observeConversation(groupId)
                .map<GroupConversation?, ConversationSnapshot> { conversation ->
                    ConversationSnapshot(conversation = conversation)
                }.catch { error ->
                    emit(ConversationSnapshot(conversation = null, error = error))
                }

        val contactsFlow: Flow<List<Contact>> =
            observeContacts()
                .onStart { emit(emptyList()) }
                .catch { emit(emptyList()) }

        val profilePicturesFlow =
            conversationFlow
                .map { snapshot ->
                    snapshot.conversation
                        ?.messages
                        .orEmpty()
                        .asSequence()
                        .mapNotNull { message -> message.senderContactId }
                        .filter(String::isNotBlank)
                        .toSet()
                }.distinctUntilChanged()
                .flatMapLatest { contactIds -> observeProfilePictures(contactIds) }

        return combine(
            conversationFlow,
            observeAdministration(groupId).onStart { emit(GroupAdministrationState()) },
            contactsFlow,
            profilePicturesFlow,
            observeGroupAvatar(groupId).map { avatar -> avatar.bytes }
        ) { conversation, administration, contacts, profilePictures, avatarBytes ->
            GroupChatContext(
                conversation = conversation.conversation,
                conversationError = conversation.error,
                administration = administration,
                contacts = contacts,
                profilePictures = profilePictures,
                avatarBytes = avatarBytes
            )
        }
    }

    private data class ConversationSnapshot(
        val conversation: GroupConversation?,
        val error: Throwable? = null
    )
}
