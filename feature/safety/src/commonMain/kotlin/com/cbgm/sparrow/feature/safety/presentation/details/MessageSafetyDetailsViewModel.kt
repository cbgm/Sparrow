package com.cbgm.sparrow.feature.safety.presentation.details

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.cbgm.sparrow.core.ui.navigation.AppRoute
import com.cbgm.sparrow.core.ui.navigation.requireRouteArgument
import com.cbgm.sparrow.core.ui.presentation.BaseViewModel
import com.cbgm.sparrow.feature.contacts.domain.usecase.BlockContactUseCase
import com.cbgm.sparrow.feature.contacts.domain.usecase.ObserveContactBlocklistUseCase
import com.cbgm.sparrow.feature.safety.presentation.details.model.MessageSafetyDetailsUiEvent
import com.cbgm.sparrow.feature.safety.presentation.details.model.MessageSafetyDetailsUiState
import com.cbgm.sparrow.feature.safety.presentation.details.model.MessageSafetyWarningLevel
import com.cbgm.sparrow.feature.safety.presentation.details.model.MessageSafetyWarningReason
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MessageSafetyDetailsViewModel(
    savedStateHandle: SavedStateHandle,
    private val blockContact: BlockContactUseCase,
    observeContactBlocklist: ObserveContactBlocklistUseCase
) : BaseViewModel() {
    private val contactId =
        savedStateHandle
            .get<String>(AppRoute.MessageSafetyDetails::contactId.name)
            ?.takeIf(String::isNotBlank)
    private val level =
        MessageSafetyWarningLevel.fromId(
            savedStateHandle.requireRouteArgument<String>(AppRoute.MessageSafetyDetails::levelId.name)
        ) ?: MessageSafetyWarningLevel.SUSPICIOUS
    private val reasons =
        savedStateHandle
            .requireRouteArgument<String>(AppRoute.MessageSafetyDetails::reasonIds.name)
            .split(',')
            .mapNotNull(MessageSafetyWarningReason::fromId)
            .distinct()
    private val focusReason =
        savedStateHandle
            .get<String>(AppRoute.MessageSafetyDetails::focusReasonId.name)
            ?.let(MessageSafetyWarningReason::fromId)
    private val blockActionState = MutableStateFlow(BlockActionState())

    val uiState: StateFlow<MessageSafetyDetailsUiState> =
        combine(
            observeContactBlocklist().map { blocklist ->
                contactId != null && blocklist.blockedContacts.any { contact -> contact.id == contactId }
            },
            blockActionState
        ) { isUserBlocked, actionState ->
            MessageSafetyDetailsUiState(
                level = level,
                reasons = reasons,
                focusReason = focusReason,
                canBlockUser = contactId != null,
                isUserBlocked = isUserBlocked,
                isBlockingUser = actionState.isBlocking,
                blockError = actionState.errorMessage
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            initialValue =
                MessageSafetyDetailsUiState(
                    level = level,
                    reasons = reasons,
                    focusReason = focusReason,
                    canBlockUser = contactId != null
                )
        )

    fun onUiEvent(event: MessageSafetyDetailsUiEvent) {
        when (event) {
            MessageSafetyDetailsUiEvent.BackClicked -> navigator.popBackStack()
            MessageSafetyDetailsUiEvent.BlockUserClicked -> blockUser()
        }
    }

    private fun blockUser() {
        val id = contactId ?: return
        if (uiState.value.isUserBlocked || blockActionState.value.isBlocking) return

        blockActionState.value = BlockActionState(isBlocking = true)
        viewModelScope.launch {
            blockContact(id)
                .onSuccess {
                    blockActionState.value = BlockActionState()
                }.onFailure { error ->
                    blockActionState.value =
                        BlockActionState(
                            errorMessage = error.message ?: "Failed to block contact"
                        )
                }
        }
    }

    private data class BlockActionState(
        val isBlocking: Boolean = false,
        val errorMessage: String? = null
    )
}
