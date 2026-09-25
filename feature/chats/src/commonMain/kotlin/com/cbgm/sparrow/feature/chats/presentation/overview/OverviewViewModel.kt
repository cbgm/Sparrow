package com.cbgm.sparrow.feature.chats.presentation.overview

import androidx.lifecycle.viewModelScope
import com.cbgm.sparrow.core.logging.ChatOpenTrace
import com.cbgm.sparrow.core.logging.SparrowLog
import com.cbgm.sparrow.core.ui.navigation.AppRoute
import com.cbgm.sparrow.core.ui.presentation.BaseViewModel
import com.cbgm.sparrow.feature.autoreply.domain.usecase.ObserveActiveAutoReplyUseCase
import com.cbgm.sparrow.feature.chats.domain.usecase.overview.ObserveConversationOverviewContextUseCase
import com.cbgm.sparrow.feature.chats.presentation.overview.mapper.toOverviewUiState
import com.cbgm.sparrow.feature.chats.presentation.overview.model.ConversationListItem
import com.cbgm.sparrow.feature.chats.presentation.overview.model.OverviewUiEvent
import com.cbgm.sparrow.feature.chats.presentation.overview.model.OverviewUiState
import com.cbgm.sparrow.feature.conversationorchestration.domain.usecase.DeleteConversationGroupUseCase
import com.cbgm.sparrow.feature.conversationorchestration.domain.usecase.DeletePeerConversationUseCase
import com.cbgm.sparrow.feature.conversationorchestration.domain.usecase.GetConversationGroupLeaveRequirementUseCase
import com.cbgm.sparrow.feature.membership.domain.model.GroupLeaveRequirement
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class OverviewViewModel(
    observeConversationContext: ObserveConversationOverviewContextUseCase,
    observeActiveAutoReply: ObserveActiveAutoReplyUseCase,
    private val deletePeerConversation: DeletePeerConversationUseCase,
    private val deleteGroupConversation: DeleteConversationGroupUseCase,
    private val getGroupLeaveRequirement: GetConversationGroupLeaveRequirementUseCase
) : BaseViewModel() {
    private val logger = SparrowLog.withTag("OverviewViewModel")
    private val error = MutableStateFlow<String?>(null)

    val uiState: StateFlow<OverviewUiState> =
        combine(
            observeConversationContext(),
            observeActiveAutoReply(),
            error
        ) { context, activeAutoReply, currentError ->
            context.conversations.toOverviewUiState(
                activeAutoReplyName = activeAutoReply?.name,
                error = currentError
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = OverviewUiState(isLoading = true)
        )

    fun onUiEvent(event: OverviewUiEvent) {
        when (event) {
            OverviewUiEvent.AutoReplyClicked -> navigator.navigateTo(AppRoute.AutoReplySettings)
            is OverviewUiEvent.ChatClicked -> openChat(event.chat)
            is OverviewUiEvent.DeleteConversation -> deleteConversation(event.conversationId)
            OverviewUiEvent.ErrorDismissed -> error.value = null
        }
    }

    private fun openChat(chat: ConversationListItem) {
        val route =
            if (chat.isGroup) {
                AppRoute.GroupConversation(chat.conversationId)
            } else {
                AppRoute.Chat(chat.conversationId, chat.contactId, chat.contactName)
            }
        ChatOpenTrace.begin(if (chat.isGroup) "group" else "direct")
        navigator.navigateTo(route)
    }

    private fun deleteConversation(conversationId: String) {
        val chat = currentConversation(conversationId) ?: return
        viewModelScope.launch {
            if (chat.isGroup) {
                deleteGroup(chat)
            } else {
                deletePeerConversation(conversationId)
                    .onFailure { failure ->
                        logger.error(failure) { "Direct conversation deletion failed" }
                        error.value = failure.message ?: "Direct conversation deletion failed"
                    }
            }
        }
    }

    private suspend fun deleteGroup(chat: ConversationListItem) {
        val requirement = getGroupLeaveRequirement(chat.conversationId)
            .getOrElse { failure ->
                logger.error(failure) { "Group leave requirement could not be resolved" }
                error.value = failure.message ?: "Group leave requirement could not be resolved"
                return
            }
        if (requirement is GroupLeaveRequirement.PromoteAdminFirst) {
            navigator.navigateTo(AppRoute.GroupDetails(chat.conversationId, requestLeave = true))
            return
        }
        deleteGroupConversation(chat.conversationId)
            .onFailure { failure ->
                logger.error(failure) { "Group conversation deletion failed" }
                error.value = failure.message ?: "Group conversation deletion failed"
            }
    }

    private fun currentConversation(conversationId: String): ConversationListItem? =
        uiState.value.conversations.firstOrNull { it.conversationId == conversationId }
}
