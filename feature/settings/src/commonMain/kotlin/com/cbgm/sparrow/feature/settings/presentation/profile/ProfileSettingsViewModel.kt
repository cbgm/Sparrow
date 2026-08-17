package com.cbgm.sparrow.feature.settings.presentation.profile

import androidx.lifecycle.viewModelScope
import com.cbgm.sparrow.core.ui.presentation.BaseViewModel
import com.cbgm.sparrow.feature.identity.domain.usecase.ObserveLocalProfilePictureUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.RemoveLocalProfilePictureUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.SetLocalProfilePictureUseCase
import com.cbgm.sparrow.feature.settings.presentation.profile.model.ProfileSettingsUiEvent
import com.cbgm.sparrow.feature.settings.presentation.profile.model.ProfileSettingsUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ProfileSettingsViewModel(
    private val observeLocalProfilePicture: ObserveLocalProfilePictureUseCase,
    private val setLocalProfilePicture: SetLocalProfilePictureUseCase,
    private val removeLocalProfilePicture: RemoveLocalProfilePictureUseCase
) : BaseViewModel() {
    private val _uiState = MutableStateFlow(ProfileSettingsUiState())
    val uiState: StateFlow<ProfileSettingsUiState> = _uiState.asStateFlow()

    init {
        observeProfilePicture()
    }

    fun onUiEvent(event: ProfileSettingsUiEvent) {
        when (event) {
            ProfileSettingsUiEvent.BackClicked -> navigator.popBackStack()
            is ProfileSettingsUiEvent.PictureSelected -> savePicture(event.bytes)
            ProfileSettingsUiEvent.RemovePictureClicked -> removePicture()
        }
    }

    private fun observeProfilePicture() {
        viewModelScope.launch {
            observeLocalProfilePicture().collect { picture ->
                _uiState.update { it.copy(profilePicture = picture) }
            }
        }
    }

    private fun savePicture(bytes: ByteArray) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }
            setLocalProfilePicture(bytes)
                .onFailure { error ->
                    _uiState.update {
                        it.copy(errorMessage = error.message ?: "Profile picture could not be saved")
                    }
                }
            _uiState.update { it.copy(isSaving = false) }
        }
    }

    private fun removePicture() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }
            removeLocalProfilePicture()
                .onFailure { error ->
                    _uiState.update {
                        it.copy(errorMessage = error.message ?: "Profile picture could not be removed")
                    }
                }
            _uiState.update { it.copy(isSaving = false) }
        }
    }
}
