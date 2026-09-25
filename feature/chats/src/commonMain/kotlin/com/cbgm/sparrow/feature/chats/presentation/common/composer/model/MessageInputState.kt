package com.cbgm.sparrow.feature.chats.presentation.common.composer.model

import com.cbgm.sparrow.feature.media.presentation.model.MediaSelection

data class MessageInputState(
    val messageText: String = "",
    val composerPreview: ComposerPreviewUi? = null,
    val indicatorType: IndicatorUiType = IndicatorUiType.NONE,
    val contactName: String = "",
    val isInputEnabled: Boolean = true,
    val isSendEnabled: Boolean = false,
    val isLocationInProgress: Boolean = false,
    val selectedMedia: List<MediaSelection> = emptyList(),
    val isGalleryEnabled: Boolean = true,
    val isCameraEnabled: Boolean = true,
    val isFileEnabled: Boolean = true
)
