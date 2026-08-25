package com.cbgm.sparrow.feature.attachments.presentation.management.model

import com.cbgm.sparrow.feature.attachments.presentation.model.MessageMediaAttachmentUi

data class AttachmentManagementUiState(
    val media: List<MessageMediaAttachmentUi> = emptyList(),
    val files: List<AttachmentFileUi> = emptyList(),
    val selectedTab: AttachmentManagementTab = AttachmentManagementTab.MEDIA,
    val isSelectionMode: Boolean = false,
    val selectedIds: Set<String> = emptySet(),
    val viewerAttachmentId: String? = null,
    val isDeleting: Boolean = false,
    val showDeleteConfirmation: Boolean = false,
    val deleteError: String? = null
) {
    val hasAttachments: Boolean
        get() = media.isNotEmpty() || files.isNotEmpty()
}
