package com.cbgm.sparrow.feature.avatar.presentation

import androidx.lifecycle.viewModelScope
import com.cbgm.sparrow.core.ui.presentation.BaseViewModel
import com.cbgm.sparrow.feature.avatar.domain.model.AvatarImage
import com.cbgm.sparrow.feature.avatar.domain.model.AvatarTarget
import com.cbgm.sparrow.feature.avatar.domain.usecase.ObserveAvatarUseCase
import com.cbgm.sparrow.feature.avatar.presentation.model.AvatarUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

internal class AvatarViewModel(
    private val target: AvatarTarget,
    private val observeAvatar: ObserveAvatarUseCase
) : BaseViewModel() {
    private val _uiState = MutableStateFlow<AvatarUiState>(AvatarUiState.Loading)
    val uiState: StateFlow<AvatarUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            observeAvatar(target).collectLatest { result ->
                _uiState.value =
                    result.fold(
                        onSuccess = { avatar -> avatar.toUiState() },
                        onFailure = AvatarUiState::Error
                    )
            }
        }
    }

    private fun AvatarImage.toUiState(): AvatarUiState =
        image?.let { bitmap ->
            AvatarUiState.Ready(
                image = bitmap,
                changedAtEpochMilliseconds = changedAtEpochMilliseconds
            )
        } ?: AvatarUiState.Empty
}
