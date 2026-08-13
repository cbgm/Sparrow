package com.cbgm.securechat.feature.chats.presentation.overview

import androidx.lifecycle.viewModelScope
import com.cbgm.securechat.core.logging.SecureChatLog
import com.cbgm.securechat.core.ui.navigation.AppRoute
import com.cbgm.securechat.core.ui.presentation.BaseViewModel
import com.cbgm.securechat.feature.chats.domain.model.group.GroupLeaveRequirement
import com.cbgm.securechat.feature.chats.domain.usecase.direct.DeleteDirectConversationUseCase
import com.cbgm.securechat.feature.chats.domain.usecase.group.DeleteGroupConversationUseCase
import com.cbgm.securechat.feature.chats.domain.usecase.group.GetGroupLeaveRequirementUseCase
import com.cbgm.securechat.feature.chats.domain.usecase.overview.ObserveConversationOverviewsUseCase
import com.cbgm.securechat.feature.chats.presentation.overview.mapper.toConversationListItem
import com.cbgm.securechat.feature.chats.presentation.overview.model.ConversationListItem
import com.cbgm.securechat.feature.chats.presentation.overview.model.OverviewUiEvent
import com.cbgm.securechat.feature.chats.presentation.overview.model.OverviewUiState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class OverviewViewModel(
    observeConversations: ObserveConversationOverviewsUseCase,
    private val deleteDirectConversation: DeleteDirectConversationUseCase,
    private val deleteGroupConversation: DeleteGroupConversationUseCase,
    private val getGroupLeaveRequirement: GetGroupLeaveRequirementUseCase
) : BaseViewModel() {
    private val logger = SecureChatLog.withTag("OverviewViewModel")

    val uiState: StateFlow<OverviewUiState> =
        observeConversations()
            .map { conversations ->
                if (conversations.isEmpty()) {
                    OverviewUiState.Empty
                } else {
                    OverviewUiState.Content(conversations.map { it.toConversationListItem() })
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
