package com.cbgm.sparrow.feature.chats.presentation.details

import androidx.lifecycle.viewModelScope
import com.cbgm.sparrow.core.ui.presentation.BaseViewModel
import com.cbgm.sparrow.feature.chats.domain.model.group.GroupAdministrationState
import com.cbgm.sparrow.feature.chats.domain.usecase.group.ObserveGroupAdministrationUseCase
import com.cbgm.sparrow.feature.chats.domain.usecase.group.ObserveGroupAvatarUseCase
import com.cbgm.sparrow.feature.chats.domain.usecase.group.ObserveGroupConversationUseCase
import com.cbgm.sparrow.feature.chats.domain.usecase.group.RemoveGroupAvatarUseCase
import com.cbgm.sparrow.feature.chats.domain.usecase.group.SetGroupAvatarUseCase
import com.cbgm.sparrow.feature.chats.presentation.details.model.GroupAvatarUiEvent
import com.cbgm.sparrow.feature.chats.presentation.details.model.GroupAvatarUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class GroupAvatarViewModel(
    private val groupId: String,
    observeConversation: ObserveGroupConversationUseCase,
    observeAdministration: ObserveGroupAdministrationUseCase,
    observeAvatar: ObserveGroupAvatarUseCase,
    private val setAvatar: SetGroupAvatarUseCase,
    private val removeGroupAvatar: RemoveGroupAvatarUseCase
) : BaseViewModel() {
    private val actionState = MutableStateFlow(ActionState())

    val uiState: StateFlow<GroupAvatarUiState> =
        combine(
            observeConversation(groupId)
                .onStart { emit(null) }
                .catch { emit(null) },
            observeAdministration(groupId)
                .onStart { emit(GroupAdministrationState()) }
                .catch { emit(GroupAdministrationState()) },
            observeAvatar(groupId),
            actionState
        ) { conversation, administration, avatar, action ->
            GroupAvatarUiState(
                title = conversation?.title.orEmpty(),
                avatarBytes = avatar.bytes,
                canEdit = administration.isLocalAdmin,
                isSaving = action.isSaving,
                errorMessage = action.errorMessage
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = GroupAvatarUiState()
        )

    fun onUiEvent(event: GroupAvatarUiEvent) {
        when (event) {
            is GroupAvatarUiEvent.AvatarSelected -> saveAvatar(event.bytes)
            GroupAvatarUiEvent.RemoveAvatarClicked -> removeCurrentAvatar()
        }
    }

    private fun saveAvatar(bytes: ByteArray) {
        if (actionState.value.isSaving || bytes.isEmpty()) return
        actionState.value = ActionState(isSaving = true)
        viewModelScope.launch {
            setAvatar(groupId, bytes)
                .onSuccess { actionState.value = ActionState() }
                .onFailure { error ->
                    actionState.value =
                        ActionState(errorMessage = error.message ?: "Group avatar could not be saved")
                }
        }
    }

    private fun removeCurrentAvatar() {
        if (actionState.value.isSaving) return
        actionState.value = ActionState(isSaving = true)
        viewModelScope.launch {
            removeGroupAvatar(groupId)
                .onSuccess { actionState.value = ActionState() }
                .onFailure { error ->
                    actionState.value =
                        ActionState(errorMessage = error.message ?: "Group avatar could not be removed")
                }
        }
    }

    private data class ActionState(
        val isSaving: Boolean = false,
        val errorMessage: String? = null
    )
}
