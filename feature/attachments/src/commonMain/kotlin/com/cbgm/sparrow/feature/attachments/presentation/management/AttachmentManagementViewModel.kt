package com.cbgm.sparrow.feature.attachments.presentation.management

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.cbgm.sparrow.core.ui.navigation.AppRoute
import com.cbgm.sparrow.core.ui.navigation.requireRouteArgument
import com.cbgm.sparrow.core.ui.presentation.BaseViewModel
import com.cbgm.sparrow.feature.attachments.domain.usecase.DeleteLocalAttachmentsUseCase
import com.cbgm.sparrow.feature.attachments.domain.usecase.LoadMessageAttachmentUseCase
import com.cbgm.sparrow.feature.attachments.domain.usecase.ObserveLocalAttachmentsUseCase
import com.cbgm.sparrow.feature.attachments.presentation.management.model.AttachmentManagementTab
import com.cbgm.sparrow.feature.attachments.presentation.management.model.AttachmentManagementUiEvent
import com.cbgm.sparrow.feature.attachments.presentation.management.model.AttachmentManagementUiState
import com.cbgm.sparrow.feature.attachments.presentation.mapper.toAttachmentManagementUi
import com.cbgm.sparrow.feature.attachments.presentation.model.MessageAttachmentUi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AttachmentManagementViewModel(
    savedStateHandle: SavedStateHandle,
    observeLocalAttachments: ObserveLocalAttachmentsUseCase,
    private val loadMessageAttachment: LoadMessageAttachmentUseCase,
    private val deleteLocalAttachments: DeleteLocalAttachmentsUseCase
) : BaseViewModel() {
    private val conversationId =
        savedStateHandle.requireRouteArgument<String>(AppRoute.AttachmentManagement::conversationId.name)
    private val localState = MutableStateFlow(AttachmentManagementLocalState())

    val uiState =
        combine(
            observeLocalAttachments(conversationId),
            localState
        ) { attachments, local ->
            val attachmentIds = attachments.mapTo(mutableSetOf()) { attachment -> attachment.id }
            val selectedIds = local.selectedIds.intersect(attachmentIds)
            val viewerAttachmentId = local.viewerAttachmentId?.takeIf(attachmentIds::contains)

            AttachmentManagementUiState(
                attachments = attachments.toAttachmentManagementUi(local.loadedBytes),
                selectedTab = local.selectedTab,
                isSelectionMode = local.isSelectionMode,
                selectedIds = selectedIds,
                viewerAttachmentId = viewerAttachmentId,
                isDeleting = local.isDeleting,
                showDeleteConfirmation = local.showDeleteConfirmation,
                deleteError = local.deleteError
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = AttachmentManagementUiState()
        )

    fun onUiEvent(event: AttachmentManagementUiEvent) {
        when (event) {
            AttachmentManagementUiEvent.BackClicked ->
                if (uiState.value.isSelectionMode) clearSelection() else navigator.popBackStack()

            is AttachmentManagementUiEvent.TabSelected ->
                localState.update { state -> state.copy(selectedTab = event.tab) }

            AttachmentManagementUiEvent.SelectionStarted ->
                localState.update { state ->
                    state.copy(
                        isSelectionMode = true,
                        selectedIds = emptySet(),
                        viewerAttachmentId = null
                    )
                }

            AttachmentManagementUiEvent.SelectionCleared -> clearSelection()
            is AttachmentManagementUiEvent.AttachmentClicked -> handleAttachmentClick(event.attachmentId)
            is AttachmentManagementUiEvent.AttachmentVisible -> ensureLoaded(event.attachmentId)
            AttachmentManagementUiEvent.DeleteSelectedClicked ->
                localState.update { state ->
                    state.copy(showDeleteConfirmation = state.selectedIds.isNotEmpty())
                }

            AttachmentManagementUiEvent.DeleteConfirmed -> deleteSelected()
            AttachmentManagementUiEvent.DeleteDismissed ->
                localState.update { state -> state.copy(showDeleteConfirmation = false) }

            AttachmentManagementUiEvent.ViewerDismissed ->
                localState.update { state -> state.copy(viewerAttachmentId = null) }

            is AttachmentManagementUiEvent.ViewerError ->
                localState.update { state -> state.copy(deleteError = event.message) }
        }
    }

    private fun handleAttachmentClick(attachmentId: String) {
        if (uiState.value.isSelectionMode) {
            toggleSelection(attachmentId)
            return
        }

        if (
            uiState.value.attachments.none { attachment ->
                attachment.id == attachmentId && attachment !is MessageAttachmentUi.FileAttachmentUi
            }
        ) {
            return
        }

        localState.update { state -> state.copy(viewerAttachmentId = attachmentId) }
        ensureLoaded(attachmentId)
    }

    private fun toggleSelection(attachmentId: String) {
        localState.update { state ->
            val updated = state.selectedIds.toMutableSet()
            if (!updated.add(attachmentId)) updated.remove(attachmentId)
            state.copy(selectedIds = updated)
        }
    }

    private fun clearSelection() {
        localState.update { state ->
            state.copy(
                isSelectionMode = false,
                selectedIds = emptySet(),
                showDeleteConfirmation = false
            )
        }
    }

    private fun ensureLoaded(attachmentId: String) {
        if (localState.value.loadedBytes.containsKey(attachmentId)) return

        viewModelScope.launch {
            loadMessageAttachment(attachmentId)
                .onSuccess { bytes ->
                    localState.update { state ->
                        state.copy(loadedBytes = state.loadedBytes + (attachmentId to bytes))
                    }
                }
        }
    }

    private fun deleteSelected() {
        val selected = localState.value.selectedIds
        if (selected.isEmpty() || localState.value.isDeleting) return

        localState.update { state ->
            state.copy(
                isDeleting = true,
                showDeleteConfirmation = false,
                deleteError = null
            )
        }

        viewModelScope.launch {
            deleteLocalAttachments(selected)
                .onSuccess {
                    localState.update { state ->
                        state.copy(
                            isSelectionMode = false,
                            selectedIds = emptySet(),
                            isDeleting = false,
                            loadedBytes = state.loadedBytes - selected
                        )
                    }
                }.onFailure { error ->
                    localState.update { state ->
                        state.copy(
                            isDeleting = false,
                            deleteError = error.message ?: "Could not delete attachments"
                        )
                    }
                }
        }
    }
}

private data class AttachmentManagementLocalState(
    val loadedBytes: Map<String, ByteArray> = emptyMap(),
    val selectedTab: AttachmentManagementTab = AttachmentManagementTab.MEDIA,
    val isSelectionMode: Boolean = false,
    val selectedIds: Set<String> = emptySet(),
    val viewerAttachmentId: String? = null,
    val isDeleting: Boolean = false,
    val showDeleteConfirmation: Boolean = false,
    val deleteError: String? = null
)
