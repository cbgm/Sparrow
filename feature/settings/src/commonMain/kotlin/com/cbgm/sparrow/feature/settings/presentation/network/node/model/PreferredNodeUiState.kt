package com.cbgm.sparrow.feature.settings.presentation.network.node.model

data class PreferredNodeUiState(
    val query: String = "",
    val preferredNodeId: String? = null,
    val nodes: List<GatewayNodeUiModel> = emptyList(),
    val isLoading: Boolean = true,
    val loadFailed: Boolean = false,
    val preferredUnlisted: Boolean = false
)

data class GatewayNodeUiModel(
    val nodeId: String,
    val shortNodeId: String,
    val address: String,
    val activeConnections: Int,
    val isPreferred: Boolean
)
