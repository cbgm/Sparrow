package com.cbgm.sparrow.feature.settings.presentation.profile

import androidx.lifecycle.viewModelScope
import com.cbgm.sparrow.core.ui.presentation.BaseViewModel
import com.cbgm.sparrow.feature.identity.domain.usecase.ObserveLocalProfilePictureUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.RemoveLocalProfilePictureUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.SetLocalProfilePictureUseCase
import com.cbgm.sparrow.feature.settings.presentation.profile.mapper.toProfileSettingsUiState
import com.cbgm.sparrow.feature.settings.presentation.profile.model.ProfileSettingsUiEvent
import com.cbgm.sparrow.feature.settings.presentation.profile.model.ProfileSettingsUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ProfileSettingsViewModel(
    observeLocalProfilePicture: ObserveLocalProfilePictureUseCase,
    private val setLocalProfilePicture: SetLocalProfilePictureUseCase,
    private val removeLocalProfilePicture: RemoveLocalProfilePictureUseCase
) : BaseViewModel() {
    private val actionState = MutableStateFlow(ProfilePictureActionState())

    val uiState: StateFlow<ProfileSettingsUiState> =
        combine(
            observeLocalProfilePicture(),
            actionState
        ) { profilePicture, action ->
            profilePicture.toProfileSettingsUiState(
                isSaving = action.isSaving,
                errorMessage = action.errorMessage
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            initialValue = ProfileSettingsUiState()
        )

    fun onUiEvent(event: ProfileSettingsUiEvent) {
        when (event) {
            ProfileSettingsUiEvent.BackClicked -> navigator.popBackStack()
            is ProfileSettingsUiEvent.PictureSelected -> savePicture(event.bytes)
            ProfileSettingsUiEvent.RemovePictureClicked -> removePicture()
        }
    }

    private fun savePicture(bytes: ByteArray) {
        if (actionState.value.isSaving) return

        viewModelScope.launch {
            actionState.value = ProfilePictureActionState(isSaving = true)
            setLocalProfilePicture(bytes)
                .onSuccess {
                    actionState.value = ProfilePictureActionState()
                }.onFailure { error ->
                    actionState.value =
                        ProfilePictureActionState(
                            errorMessage = error.message ?: "Profile picture could not be saved"
                        )
                }
        }
    }

    private fun removePicture() {
        if (actionState.value.isSaving) return

        viewModelScope.launch {
            actionState.value = ProfilePictureActionState(isSaving = true)
            removeLocalProfilePicture()
                .onSuccess {
                    actionState.value = ProfilePictureActionState()
                }.onFailure { error ->
                    actionState.value =
                        ProfilePictureActionState(
                            errorMessage = error.message ?: "Profile picture could not be removed"
                        )
                }
        }
    }

    private data class ProfilePictureActionState(
        val isSaving: Boolean = false,
        val errorMessage: String? = null
    )
}
