package com.cbgm.sparrow.feature.search.presentation.model

data class MessageSearchUiState(
    val query: String = "",
    val results: List<MessageSearchResultUi> = emptyList(),
    val isSearching: Boolean = false,
    val mode: MessageSearchMode = MessageSearchMode.EXACT_ONLY,
    val searchFailed: Boolean = false
)
