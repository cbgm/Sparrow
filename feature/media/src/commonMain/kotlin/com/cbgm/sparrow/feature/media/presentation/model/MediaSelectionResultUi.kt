package com.cbgm.sparrow.feature.media.presentation.model

sealed interface MediaSelectionResultUi {
    data class Selected(
        val media: List<MediaSelectionUi>
    ) : MediaSelectionResultUi

    data object Dismissed : MediaSelectionResultUi

    data class Error(
        val message: String
    ) : MediaSelectionResultUi
}
