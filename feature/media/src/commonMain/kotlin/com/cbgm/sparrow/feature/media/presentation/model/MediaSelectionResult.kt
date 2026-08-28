package com.cbgm.sparrow.feature.media.presentation.model

sealed interface MediaSelectionResult {
    data class Selected(
        val media: List<MediaSelection>
    ) : MediaSelectionResult

    data object Dismissed : MediaSelectionResult

    data class Error(
        val message: String
    ) : MediaSelectionResult
}
