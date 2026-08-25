package com.cbgm.sparrow.feature.attachments.presentation.storage

import androidx.lifecycle.viewModelScope
import com.cbgm.sparrow.core.ui.navigation.AppRoute
import com.cbgm.sparrow.core.ui.presentation.BaseViewModel
import com.cbgm.sparrow.feature.attachments.domain.usecase.ObserveAttachmentStorageSummariesUseCase
import com.cbgm.sparrow.feature.attachments.presentation.storage.model.AttachmentStorageUiEvent
import com.cbgm.sparrow.feature.attachments.presentation.storage.model.AttachmentStorageUiState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class AttachmentStorageViewModel(
    observeAttachmentStorageSummaries: ObserveAttachmentStorageSummariesUseCase
) : BaseViewModel() {
    val uiState =
        observeAttachmentStorageSummaries()
            .map { summaries -> AttachmentStorageUiState.Content(summaries) as AttachmentStorageUiState }
            .catch { error -> emit(AttachmentStorageUiState.Error(error.message ?: "Could not load attachment storage")) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = AttachmentStorageUiState.Loading
            )

    fun onUiEvent(event: AttachmentStorageUiEvent) {
        when (event) {
            AttachmentStorageUiEvent.BackClicked -> navigator.popBackStack()
            is AttachmentStorageUiEvent.ConversationClicked ->
                navigator.navigateTo(AppRoute.AttachmentManagement(event.conversationId))
        }
    }
}
