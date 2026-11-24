package com.cbgm.sparrow.feature.settings.presentation.network.node.model

data class PreferredNodeUiState(
    val query: String = "",
    val preferredNodeId: String? = null,
    val nodes: List<GatewayNodeUi> = emptyList(),
    val isLoading: Boolean = true,
    val loadFailed: Boolean = false,
    val preferredUnlisted: Boolean = false
)

data class GatewayNodeUi(
    val nodeId: String,
    val shortNodeId: String,
    val address: String,
    val isPreferred: Boolean
)
