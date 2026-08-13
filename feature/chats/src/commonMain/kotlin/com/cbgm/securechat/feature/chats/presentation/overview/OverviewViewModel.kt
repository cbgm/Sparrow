package com.cbgm.securechat.feature.chats.presentation.overview

import androidx.lifecycle.viewModelScope
import com.cbgm.securechat.core.logging.SecureChatLog
import com.cbgm.securechat.core.ui.navigation.AppRoute
import com.cbgm.securechat.core.ui.presentation.BaseViewModel
import com.cbgm.securechat.feature.chats.domain.model.GroupLeaveRequirement
import com.cbgm.securechat.feature.chats.domain.usecase.DeleteConversationUseCase
import com.cbgm.securechat.feature.chats.domain.usecase.GetGroupLeaveRequirementUseCase
import com.cbgm.securechat.feature.chats.domain.usecase.ObserveConversationsUseCase
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
    observeConversations: ObserveConversationsUseCase,
    private val deleteConversationUseCase: DeleteConversationUseCase,
    private val getGroupLeaveRequirement: GetGroupLeaveRequirementUseCase
) : BaseViewModel() {
    private val logger = SecureChatLog.withTag("OverviewViewModel")
    val uiState: StateFlow<OverviewUiState> =
        observeConversations()
            .map { conversations ->
                if (conversations.isEmpty()) {
                    return@map OverviewUiState.Empty
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
                AppRoute.GroupConversation(conversationId = chat.conversationId)
            } else {
                AppRoute.Chat(
                    conversationId = chat.conversationId,
                    contactId = chat.contactId,
                    contactName = chat.contactName
                )
            }

        navigator.navigateTo(route)
    }

    private fun deleteConversation(conversationId: String) {
        viewModelScope.launch {
            val chat =
                (uiState.value as? OverviewUiState.Content)
                    ?.conversations
                    ?.firstOrNull { item -> item.conversationId == conversationId }
            if (chat?.isGroup == true) {
                getGroupLeaveRequirement(conversationId)
                    .onSuccess { requirement ->
                        if (requirement is GroupLeaveRequirement.PromoteAdminFirst) {
                            navigator.navigateTo(
                                AppRoute.GroupDetails(
                                    conversationId = conversationId,
                                    requestLeave = true
                                )
                            )
                            return@launch
                        }
                    }.onFailure { error ->
                        logger.error(error) { "Group leave requirement could not be resolved" }
                        return@launch
                    }
            }
            deleteConversationUseCase(conversationId)
                .onFailure { error ->
                    logger.error(error) { "Conversation deletion failed" }
                }
        }
    }
}
