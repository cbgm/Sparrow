package com.cbgm.sparrow.feature.attachments.presentation

import androidx.lifecycle.viewModelScope
import com.cbgm.sparrow.core.ui.presentation.BaseViewModel
import com.cbgm.sparrow.feature.attachments.domain.model.AttachmentTarget
import com.cbgm.sparrow.feature.attachments.domain.usecase.LoadAttachmentContentUseCase
import com.cbgm.sparrow.feature.attachments.presentation.model.AttachmentUiState
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AttachmentViewModel(
    private val target: AttachmentTarget,
    private val loadAttachmentContent: LoadAttachmentContentUseCase
) : BaseViewModel() {
    private val _uiState = MutableStateFlow<AttachmentUiState>(AttachmentUiState.Idle)
    val uiState: StateFlow<AttachmentUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null

    fun load() {
        if (_uiState.value is AttachmentUiState.Ready || loadJob?.isActive == true) return

        loadJob =
            viewModelScope.launch {
                _uiState.value = AttachmentUiState.Loading
                _uiState.value =
                    loadAttachmentContent(target).fold(
                        onSuccess = AttachmentUiState::Ready,
                        onFailure = AttachmentUiState::Error
                    )
            }
    }
}
