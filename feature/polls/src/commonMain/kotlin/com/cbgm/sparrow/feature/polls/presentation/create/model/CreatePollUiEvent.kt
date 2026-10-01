package com.cbgm.sparrow.feature.polls.presentation.create.model

import com.cbgm.sparrow.feature.media.presentation.model.MediaSelectionResultUi

sealed interface CreatePollUiEvent {
    data object BackClicked : CreatePollUiEvent

    data object AddOptionClicked : CreatePollUiEvent

    data object CreateClicked : CreatePollUiEvent

    data object ExpiryCleared : CreatePollUiEvent

    data class QuestionChanged(
        val value: String
    ) : CreatePollUiEvent

    data class DescriptionChanged(
        val value: String
    ) : CreatePollUiEvent

    data class OptionChanged(
        val id: String,
        val value: String
    ) : CreatePollUiEvent

    data class RemoveOptionClicked(
        val id: String
    ) : CreatePollUiEvent

    data class MediaSelectionChanged(
        val result: MediaSelectionResultUi
    ) : CreatePollUiEvent

    data class RemoveMediaClicked(
        val id: String
    ) : CreatePollUiEvent

    data class ExpiryEnabledChanged(
        val enabled: Boolean
    ) : CreatePollUiEvent

    data class ExpiryDateChanged(
        val value: String
    ) : CreatePollUiEvent

    data class ExpiryTimeChanged(
        val value: String
    ) : CreatePollUiEvent

    data class MultipleSelectionChanged(
        val enabled: Boolean
    ) : CreatePollUiEvent

    data class VoteChangeChanged(
        val enabled: Boolean
    ) : CreatePollUiEvent

    data class AnonymousChanged(
        val enabled: Boolean
    ) : CreatePollUiEvent
}
