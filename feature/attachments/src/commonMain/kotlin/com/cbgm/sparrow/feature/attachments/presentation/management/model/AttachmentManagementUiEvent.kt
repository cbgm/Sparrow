package com.cbgm.sparrow.feature.attachments.presentation.management.model

sealed interface AttachmentManagementUiEvent {
    data object BackClicked : AttachmentManagementUiEvent

    data class TabSelected(
        val tab: AttachmentManagementTab
    ) : AttachmentManagementUiEvent

    data object SelectionStarted : AttachmentManagementUiEvent

    data object SelectionCleared : AttachmentManagementUiEvent

    data class AttachmentClicked(
        val attachmentId: String
    ) : AttachmentManagementUiEvent

    data class AttachmentVisible(
        val attachmentId: String
    ) : AttachmentManagementUiEvent

    data object DeleteSelectedClicked : AttachmentManagementUiEvent

    data object DeleteConfirmed : AttachmentManagementUiEvent

    data object DeleteDismissed : AttachmentManagementUiEvent

    data object ViewerDismissed : AttachmentManagementUiEvent

    data class ViewerError(
        val message: String
    ) : AttachmentManagementUiEvent
}
