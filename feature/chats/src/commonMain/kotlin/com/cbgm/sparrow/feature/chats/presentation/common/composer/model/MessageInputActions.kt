package com.cbgm.sparrow.feature.chats.presentation.common.composer.model

import com.cbgm.sparrow.feature.media.presentation.model.MediaSelectionSource

data class MessageInputActions(
    val onValueChange: (String) -> Unit,
    val onSendClick: () -> Unit,
    val onCancelPreview: () -> Unit = {},
    val onSelectionClick: (MediaSelectionSource) -> Unit = {},
    val onMediaRemove: (String) -> Unit = {},
    val onClickCamera: () -> Unit = {},
    val onClickFile: () -> Unit = {},
    val onClickGallery: () -> Unit = {},
    val onClickContact: () -> Unit = {},
    val onClickLocation: () -> Unit = {},
    val onVoiceSendClick: () -> Unit = {}
)
