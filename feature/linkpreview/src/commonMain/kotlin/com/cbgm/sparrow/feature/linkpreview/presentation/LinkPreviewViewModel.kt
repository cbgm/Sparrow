package com.cbgm.sparrow.feature.linkpreview.presentation

import androidx.lifecycle.viewModelScope
import com.cbgm.sparrow.core.logging.SparrowLog
import com.cbgm.sparrow.core.ui.presentation.BaseViewModel
import com.cbgm.sparrow.feature.linkpreview.domain.usecase.GetLinkPreviewUseCase
import com.cbgm.sparrow.feature.linkpreview.presentation.mapper.toUi
import com.cbgm.sparrow.feature.linkpreview.presentation.model.LinkPreviewUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LinkPreviewViewModel(
    private val url: String,
    private val getLinkPreview: GetLinkPreviewUseCase
) : BaseViewModel() {
    private val _uiState = MutableStateFlow<LinkPreviewUiState>(LinkPreviewUiState.Loading)
    val uiState: StateFlow<LinkPreviewUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    private fun load() {
        viewModelScope.launch {
            getLinkPreview(url)
                .onSuccess { preview ->
                    _uiState.value = LinkPreviewUiState.Success(preview.toUi())
                }.onFailure { throwable ->
                    SparrowLog.error("LinkPreviewViewModel", "Link preview could not be loaded", throwable)
                    _uiState.value = LinkPreviewUiState.Error(throwable)
                }
        }
    }
}
