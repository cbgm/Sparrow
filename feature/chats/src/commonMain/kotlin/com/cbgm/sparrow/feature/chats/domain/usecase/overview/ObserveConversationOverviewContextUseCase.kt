package com.cbgm.sparrow.feature.chats.domain.usecase.overview

import com.cbgm.sparrow.core.protocol.profile.RemoteProfilePictureProvider
import com.cbgm.sparrow.feature.chats.domain.model.overview.ConversationOverviewContext
import com.cbgm.sparrow.feature.chats.domain.model.overview.ConversationOverviewType
import com.cbgm.sparrow.feature.chats.domain.repository.group.GroupAvatarRepository
import com.cbgm.sparrow.feature.chats.domain.repository.overview.ConversationOverviewRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart

@OptIn(ExperimentalCoroutinesApi::class)
class ObserveConversationOverviewContextUseCase(
    private val conversationRepository: ConversationOverviewRepository,
    private val remoteProfilePictureProvider: RemoteProfilePictureProvider,
    private val groupAvatarRepository: GroupAvatarRepository
) {
    operator fun invoke(): Flow<ConversationOverviewContext> =
        conversationRepository
            .observeAll()
            .flatMapLatest { conversations ->
                val directContactIds =
                    conversations
                        .asSequence()
                        .filter { it.type == ConversationOverviewType.DIRECT }
                        .map { it.contactId }
                        .filter(String::isNotBlank)
                        .toSet()
                val groupIds =
                    conversations
                        .asSequence()
                        .filter { it.type == ConversationOverviewType.GROUP }
                        .map { it.id }
                        .filter(String::isNotBlank)
                        .toSet()

                combine(
                    observeProfilePictures(directContactIds),
                    observeGroupAvatars(groupIds)
                ) { profilePictures, groupAvatars ->
                    ConversationOverviewContext(
                        conversations = conversations,
                        profilePictures = profilePictures,
                        groupAvatars = groupAvatars
                    )
                }
            }

    private fun observeProfilePictures(contactIds: Set<String>): Flow<Map<String, ByteArray?>> {
        if (contactIds.isEmpty()) return flowOf(emptyMap())

        return combine(
            contactIds.map { contactId ->
                remoteProfilePictureProvider
                    .observe(contactId)
                    .map { picture -> contactId to picture.bytes }
                    .catch { emit(contactId to null) }
                    .onStart { emit(contactId to null) }
            }
        ) { pictures -> pictures.toMap() }
    }

    private fun observeGroupAvatars(groupIds: Set<String>): Flow<Map<String, ByteArray?>> {
        if (groupIds.isEmpty()) return flowOf(emptyMap())

        return combine(
            groupIds.map { groupId ->
                groupAvatarRepository
                    .observe(groupId)
                    .map { avatar -> groupId to avatar.bytes }
                    .catch { emit(groupId to null) }
                    .onStart { emit(groupId to null) }
            }
        ) { entries -> entries.toMap() }
    }
}
