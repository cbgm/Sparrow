package com.cbgm.sparrow.feature.search.presentation.overview.model

sealed interface MessageSearchUiEvent {
    data class QueryChanged(
        val query: String
    ) : MessageSearchUiEvent

    data object ClearQueryClicked : MessageSearchUiEvent

    data class ResultClicked(
        val messageId: String
    ) : MessageSearchUiEvent

    data object BackClicked : MessageSearchUiEvent
}
