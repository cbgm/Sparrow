package com.cbgm.sparrow.feature.search.domain.model

sealed interface SemanticSearchState {
    data object Disabled : SemanticSearchState

    data object Preparing : SemanticSearchState

    data class DownloadingModel(
        val progress: Float?
    ) : SemanticSearchState

    data class BuildingIndex(
        val processed: Int,
        val total: Int
    ) : SemanticSearchState

    data object Ready : SemanticSearchState

    data class Failed(
        val reason: String
    ) : SemanticSearchState
}
