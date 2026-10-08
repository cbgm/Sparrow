package com.cbgm.sparrow.feature.media.presentation.filepicker.model

import com.cbgm.sparrow.feature.media.presentation.model.MediaSelectionUi

sealed interface FilePickerSessionResultUi {
    val sessionId: String

    data class Completed(
        override val sessionId: String,
        val media: List<MediaSelectionUi>
    ) : FilePickerSessionResultUi

    data class Dismissed(
        override val sessionId: String
    ) : FilePickerSessionResultUi

    data class Failed(
        override val sessionId: String,
        val message: String
    ) : FilePickerSessionResultUi
}
