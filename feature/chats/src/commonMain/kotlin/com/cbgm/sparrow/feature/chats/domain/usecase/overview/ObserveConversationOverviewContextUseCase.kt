package com.cbgm.sparrow.feature.chats.domain.usecase.overview

import com.cbgm.sparrow.feature.chats.domain.model.overview.ConversationOverviewContext
import com.cbgm.sparrow.feature.chats.domain.model.overview.ConversationOverviewType
import com.cbgm.sparrow.feature.chats.domain.usecase.group.ObserveGroupAvatarsUseCase
import com.cbgm.sparrow.feature.chats.domain.usecase.profile.ObserveRemoteProfilePicturesUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest

@OptIn(ExperimentalCoroutinesApi::class)
class ObserveConversationOverviewContextUseCase(
    private val observeConversations: ObserveConversationOverviewsUseCase,
    private val observeProfilePictures: ObserveRemoteProfilePicturesUseCase,
    private val observeGroupAvatars: ObserveGroupAvatarsUseCase
) {
    operator fun invoke(): Flow<ConversationOverviewContext> =
        observeConversations()
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
}
