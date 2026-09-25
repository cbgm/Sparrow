package com.cbgm.sparrow.feature.avatar.presentation.editor

import androidx.lifecycle.viewModelScope
import com.cbgm.sparrow.core.logging.SparrowLog
import com.cbgm.sparrow.core.ui.presentation.BaseViewModel
import com.cbgm.sparrow.feature.avatar.domain.model.AvatarEditResult
import com.cbgm.sparrow.feature.avatar.domain.model.ProfilePictureCropRegion
import com.cbgm.sparrow.feature.avatar.domain.usecase.ClearAvatarEditorUseCase
import com.cbgm.sparrow.feature.avatar.domain.usecase.CropAvatarEditorSourceUseCase
import com.cbgm.sparrow.feature.avatar.domain.usecase.PrepareAvatarEditorSourceUseCase
import com.cbgm.sparrow.feature.avatar.presentation.editor.model.AvatarEditorUiState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal class AvatarEditorViewModel(
    private val prepareSource: PrepareAvatarEditorSourceUseCase,
    private val cropSource: CropAvatarEditorSourceUseCase,
    private val clearEditor: ClearAvatarEditorUseCase
) : BaseViewModel() {
    private val _uiState = MutableStateFlow(AvatarEditorUiState())
    val uiState: StateFlow<AvatarEditorUiState> = _uiState.asStateFlow()

    private val _result = MutableSharedFlow<AvatarEditResult>(extraBufferCapacity = 1)
    val result: SharedFlow<AvatarEditResult> = _result.asSharedFlow()

    fun onSourceSelected(bytes: ByteArray) {
        if (bytes.isEmpty() || _uiState.value.isPreparing) return

        viewModelScope.launch {
            _uiState.value = AvatarEditorUiState(isPreparing = true)
            prepareSource(bytes)
                .onSuccess { source ->
                    _uiState.value = AvatarEditorUiState(image = source.image)
                }.onFailure { error ->
                    SparrowLog.error("AvatarEditorViewModel", "Could not prepare profile picture", error)
                    _uiState.value = AvatarEditorUiState(error = error)
                }
        }
    }

    fun onCropConfirmed(cropRegion: ProfilePictureCropRegion) {
        val current = _uiState.value
        if (current.image == null || current.isCropping) return

        viewModelScope.launch {
            _uiState.value = current.copy(isCropping = true, error = null)
            cropSource(cropRegion)
                .onSuccess { result ->
                    _uiState.value = AvatarEditorUiState()
                    _result.emit(result)
                }.onFailure { error ->
                    SparrowLog.error("AvatarEditorViewModel", "Could not crop profile picture", error)
                    _uiState.value = current.copy(isCropping = false, error = error)
                }
        }
    }

    fun clear() {
        clearEditor()
        _uiState.value = AvatarEditorUiState()
    }
}
