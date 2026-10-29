package com.cbgm.sparrow.feature.settings.presentation.network.node.model

sealed interface PreferredNodeUiEvent {
    data object BackClicked : PreferredNodeUiEvent

    data object Refresh : PreferredNodeUiEvent

    data object AutomaticSelected : PreferredNodeUiEvent

    data object QueryCleared : PreferredNodeUiEvent

    data class QueryChanged(
        val query: String
    ) : PreferredNodeUiEvent

    data class NodeSelected(
        val nodeId: String
    ) : PreferredNodeUiEvent
}
