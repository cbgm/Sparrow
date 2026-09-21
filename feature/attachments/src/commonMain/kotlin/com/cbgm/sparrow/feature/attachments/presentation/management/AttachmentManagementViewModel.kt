package com.cbgm.sparrow.feature.attachments.presentation.management

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.cbgm.sparrow.core.logging.SparrowLog
import com.cbgm.sparrow.core.ui.navigation.AppRoute
import com.cbgm.sparrow.core.ui.navigation.requireRouteArgument
import com.cbgm.sparrow.core.ui.presentation.BaseViewModel
import com.cbgm.sparrow.feature.attachments.domain.usecase.DeleteLocalAttachmentsUseCase
import com.cbgm.sparrow.feature.attachments.domain.usecase.ObserveLocalAttachmentsUseCase
import com.cbgm.sparrow.feature.attachments.presentation.management.model.AttachmentManagementTab
import com.cbgm.sparrow.feature.attachments.presentation.management.model.AttachmentManagementUiEvent
import com.cbgm.sparrow.feature.attachments.presentation.management.model.AttachmentManagementUiState
import com.cbgm.sparrow.feature.attachments.presentation.mapper.toMessageAttachmentsUi
import com.cbgm.sparrow.feature.attachments.presentation.model.MessageAttachmentUi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AttachmentManagementViewModel(
    savedStateHandle: SavedStateHandle,
    observeLocalAttachments: ObserveLocalAttachmentsUseCase,
    private val deleteLocalAttachments: DeleteLocalAttachmentsUseCase
) : BaseViewModel() {
    private val conversationId =
        savedStateHandle.requireRouteArgument<String>(AppRoute.AttachmentManagement::conversationId.name)
    private val localState = MutableStateFlow(AttachmentManagementLocalState())

    // Map attachments and index IDs only when the source emits, not on selection changes.
    // uiState is the only exposed StateFlow; the snapshot is an intermediate Flow value.
    val uiState = combine(
        observeLocalAttachments(conversationId).map { records ->
            val items = records.toMessageAttachmentsUi()
            AttachmentSnapshot(items, items.mapTo(mutableSetOf()) { it.id })
        },
        localState
    ) { snapshot, local ->
        val selectedIds = local.selectedIds.intersect(snapshot.ids)
        val viewerAttachmentId = local.viewerAttachmentId?.takeIf(snapshot.ids::contains)

        AttachmentManagementUiState(
            attachments = snapshot.items,
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
                            isDeleting = false
                        )
                    }
                }.onFailure { error ->
                    SparrowLog.error("AttachmentManagementViewModel", "Could not delete attachments", error)
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
    val selectedTab: AttachmentManagementTab = AttachmentManagementTab.MEDIA,
    val isSelectionMode: Boolean = false,
    val selectedIds: Set<String> = emptySet(),
    val viewerAttachmentId: String? = null,
    val isDeleting: Boolean = false,
    val showDeleteConfirmation: Boolean = false,
    val deleteError: String? = null
)

private data class AttachmentSnapshot(
    val items: List<MessageAttachmentUi>,
    val ids: Set<String>
)
