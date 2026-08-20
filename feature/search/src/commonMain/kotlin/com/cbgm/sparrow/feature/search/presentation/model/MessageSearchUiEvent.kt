package com.cbgm.sparrow.feature.search.presentation.model

sealed interface MessageSearchUiEvent {
    data class QueryChanged(
        val query: String
    ) : MessageSearchUiEvent

    data object ClearQueryClicked : MessageSearchUiEvent

    data object BackClicked : MessageSearchUiEvent
}
