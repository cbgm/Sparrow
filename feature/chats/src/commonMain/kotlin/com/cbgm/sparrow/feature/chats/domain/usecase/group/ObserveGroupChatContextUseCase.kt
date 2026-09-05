package com.cbgm.sparrow.feature.chats.domain.usecase.group

import com.cbgm.sparrow.core.protocol.profile.RemoteProfilePictureProvider
import com.cbgm.sparrow.feature.chats.domain.model.MessageHistoryCursor
import com.cbgm.sparrow.feature.chats.domain.model.group.GroupAdministrationState
import com.cbgm.sparrow.feature.chats.domain.model.group.GroupChatContext
import com.cbgm.sparrow.feature.chats.domain.model.group.GroupConversation
import com.cbgm.sparrow.feature.chats.domain.repository.group.GroupAvatarRepository
import com.cbgm.sparrow.feature.chats.domain.repository.group.GroupConversationRepository
import com.cbgm.sparrow.feature.chats.domain.repository.group.GroupMembershipRepository
import com.cbgm.sparrow.feature.contacts.domain.model.Contact
import com.cbgm.sparrow.feature.contacts.domain.repository.ContactRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart

@OptIn(ExperimentalCoroutinesApi::class)
class ObserveGroupChatContextUseCase(
    private val conversationRepository: GroupConversationRepository,
    private val membershipRepository: GroupMembershipRepository,
    private val contactRepository: ContactRepository,
    private val remoteProfilePictureProvider: RemoteProfilePictureProvider,
    private val avatarRepository: GroupAvatarRepository
) {
    operator fun invoke(
        groupId: String,
        oldestCursor: MessageHistoryCursor? = null
    ): Flow<GroupChatContext> {
        val conversationFlow =
            conversationRepository
                .observe(groupId, oldestCursor)
                .map<GroupConversation?, ConversationSnapshot> { conversation ->
                    ConversationSnapshot(conversation = conversation)
                }.catch { error ->
                    emit(ConversationSnapshot(conversation = null, error = error))
                }

        val contactsFlow: Flow<List<Contact>> =
            contactRepository
                .observeContacts()
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
                .flatMapLatest(::observeProfilePictures)

        return combine(
            conversationFlow,
            membershipRepository
                .observeAdministration(groupId)
                .onStart { emit(GroupAdministrationState()) },
            contactsFlow,
            profilePicturesFlow,
            avatarRepository.observe(groupId).map { avatar -> avatar.bytes }
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

    private fun observeProfilePictures(contactIds: Set<String>): Flow<Map<String, ByteArray?>> {
        val ids = contactIds.filter(String::isNotBlank).distinct()
        if (ids.isEmpty()) return flowOf(emptyMap())

        return combine(
            ids.map { contactId ->
                remoteProfilePictureProvider
                    .observe(contactId)
                    .map { picture -> contactId to picture.bytes }
                    .catch { emit(contactId to null) }
                    .onStart { emit(contactId to null) }
            }
        ) { pictures -> pictures.toMap() }
    }

    private data class ConversationSnapshot(
        val conversation: GroupConversation?,
        val error: Throwable? = null
    )
}
