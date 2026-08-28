package com.cbgm.sparrow.feature.attachments.presentation.management.model

import com.cbgm.sparrow.feature.attachments.presentation.model.MessageAttachmentUi

data class AttachmentManagementUiState(
    val attachments: List<MessageAttachmentUi> = emptyList(),
    val selectedTab: AttachmentManagementTab = AttachmentManagementTab.MEDIA,
    val isSelectionMode: Boolean = false,
    val selectedIds: Set<String> = emptySet(),
    val viewerAttachmentId: String? = null,
    val isDeleting: Boolean = false,
    val showDeleteConfirmation: Boolean = false,
    val deleteError: String? = null
) {
    val hasAttachments: Boolean
        get() = attachments.isNotEmpty()
}
