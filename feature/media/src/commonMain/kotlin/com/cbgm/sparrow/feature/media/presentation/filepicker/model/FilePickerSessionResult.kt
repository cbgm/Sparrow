package com.cbgm.sparrow.feature.media.presentation.filepicker.model

import com.cbgm.sparrow.feature.media.presentation.model.MediaSelection

sealed interface FilePickerSessionResult {
    val sessionId: String

    data class Completed(
        override val sessionId: String,
        val media: List<MediaSelection>
    ) : FilePickerSessionResult

    data class Dismissed(
        override val sessionId: String
    ) : FilePickerSessionResult

    data class Failed(
        override val sessionId: String,
        val message: String
    ) : FilePickerSessionResult
}
