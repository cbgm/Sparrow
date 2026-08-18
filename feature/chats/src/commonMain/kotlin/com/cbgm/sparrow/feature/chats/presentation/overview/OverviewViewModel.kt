package com.cbgm.sparrow.feature.chats.presentation.overview

import androidx.lifecycle.viewModelScope
import com.cbgm.sparrow.core.logging.SparrowLog
import com.cbgm.sparrow.core.ui.navigation.AppRoute
import com.cbgm.sparrow.core.ui.presentation.BaseViewModel
import com.cbgm.sparrow.feature.chats.domain.model.group.GroupLeaveRequirement
import com.cbgm.sparrow.feature.chats.domain.usecase.direct.DeleteDirectConversationUseCase
import com.cbgm.sparrow.feature.chats.domain.usecase.group.DeleteGroupConversationUseCase
import com.cbgm.sparrow.feature.chats.domain.usecase.group.GetGroupLeaveRequirementUseCase
import com.cbgm.sparrow.feature.chats.domain.usecase.group.ObserveGroupAvatarsUseCase
import com.cbgm.sparrow.feature.chats.domain.usecase.overview.ObserveConversationOverviewsUseCase
import com.cbgm.sparrow.feature.chats.domain.usecase.profile.ObserveRemoteProfilePicturesUseCase
import com.cbgm.sparrow.feature.chats.presentation.overview.mapper.directContactIds
import com.cbgm.sparrow.feature.chats.presentation.overview.mapper.groupIds
import com.cbgm.sparrow.feature.chats.presentation.overview.mapper.toUiState
import com.cbgm.sparrow.feature.chats.presentation.overview.model.ConversationListItem
import com.cbgm.sparrow.feature.chats.presentation.overview.model.OverviewUiEvent
import com.cbgm.sparrow.feature.chats.presentation.overview.model.OverviewUiState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class OverviewViewModel(
    observeConversations: ObserveConversationOverviewsUseCase,
    observeProfilePictures: ObserveRemoteProfilePicturesUseCase,
    observeGroupAvatars: ObserveGroupAvatarsUseCase,
    private val deleteDirectConversation: DeleteDirectConversationUseCase,
    private val deleteGroupConversation: DeleteGroupConversationUseCase,
    private val getGroupLeaveRequirement: GetGroupLeaveRequirementUseCase
) : BaseViewModel() {
    private val logger = SparrowLog.withTag("OverviewViewModel")

    val uiState: StateFlow<OverviewUiState> =
        observeConversations()
            .flatMapLatest { conversations ->
                combine(
                    observeProfilePictures(conversations.directContactIds()),
                    observeGroupAvatars(conversations.groupIds())
                ) { profilePictures, groupAvatars ->
                    conversations.toUiState(
                        profilePictures = profilePictures,
                        groupAvatars = groupAvatars
                    )
                }
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = OverviewUiState.Loading
            )

    fun onUiEvent(event: OverviewUiEvent) {
        when (event) {
            is OverviewUiEvent.ChatClicked -> openChat(event.chat)
            is OverviewUiEvent.DeleteConversation -> deleteConversation(event.conversationId)
        }
    }

    private fun openChat(chat: ConversationListItem) {
        val route =
            if (chat.isGroup) {
                AppRoute.GroupConversation(chat.conversationId)
            } else {
                AppRoute.Chat(chat.conversationId, chat.contactId, chat.contactName)
            }
        navigator.navigateTo(route)
    }

    private fun deleteConversation(conversationId: String) {
        val chat = currentConversation(conversationId) ?: return
        viewModelScope.launch {
            if (chat.isGroup) {
                deleteGroup(chat)
            } else {
                deleteDirectConversation(conversationId)
                    .onFailure { error -> logger.error(error) { "Direct conversation deletion failed" } }
            }
        }
    }

    private suspend fun deleteGroup(chat: ConversationListItem) {
        val requirement = getGroupLeaveRequirement(chat.conversationId)
            .getOrElse { error ->
                logger.error(error) { "Group leave requirement could not be resolved" }
                return
            }
        if (requirement is GroupLeaveRequirement.PromoteAdminFirst) {
            navigator.navigateTo(AppRoute.GroupDetails(chat.conversationId, requestLeave = true))
            return
        }
        deleteGroupConversation(chat.conversationId)
            .onFailure { error -> logger.error(error) { "Group conversation deletion failed" } }
    }

    private fun currentConversation(conversationId: String): ConversationListItem? =
        (uiState.value as? OverviewUiState.Content)
            ?.conversations
            ?.firstOrNull { it.conversationId == conversationId }
}
