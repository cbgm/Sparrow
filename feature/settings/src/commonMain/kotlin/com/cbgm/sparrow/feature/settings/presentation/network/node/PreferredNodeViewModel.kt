package com.cbgm.sparrow.feature.settings.presentation.network.node

import androidx.lifecycle.viewModelScope
import com.cbgm.sparrow.core.ui.presentation.BaseViewModel
import com.cbgm.sparrow.core.util.logging.SparrowLog
import com.cbgm.sparrow.feature.settings.presentation.network.node.model.GatewayNodeUiModel
import com.cbgm.sparrow.feature.settings.presentation.network.node.model.PreferredNodeUiEvent
import com.cbgm.sparrow.feature.settings.presentation.network.node.model.PreferredNodeUiState
import com.cbgm.sparrow.feature.transport.discovery.GatewayNode
import com.cbgm.sparrow.feature.transport.domain.usecase.GetGatewayNodesUseCase
import com.cbgm.sparrow.feature.transport.domain.usecase.ObservePreferredNodeUseCase
import com.cbgm.sparrow.feature.transport.domain.usecase.SetPreferredNodeUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PreferredNodeViewModel(
    private val getGatewayNodes: GetGatewayNodesUseCase,
    observePreferredNode: ObservePreferredNodeUseCase,
    private val setPreferredNode: SetPreferredNodeUseCase
) : BaseViewModel() {
    private val logger = SparrowLog.withTag("PreferredNodeViewModel")
    private val loadState = MutableStateFlow(NodeLoadState())
    private val query = MutableStateFlow("")

    val uiState: StateFlow<PreferredNodeUiState> =
        combine(loadState, query, observePreferredNode()) { load, currentQuery, preferredNodeId ->
            PreferredNodeUiState(
                query = currentQuery,
                preferredNodeId = preferredNodeId,
                nodes =
                    load.nodes
                        .filter { node -> node.matches(currentQuery) }
                        .sortedByDescending { node -> node.nodeId == preferredNodeId }
                        .map { node -> node.toUiModel(preferredNodeId) },
                isLoading = load.isLoading,
                loadFailed = load.failed,
                preferredUnlisted =
                    !load.isLoading && preferredNodeId != null && load.nodes.none { it.nodeId == preferredNodeId }
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLISECONDS), PreferredNodeUiState())

    init {
        load(forceRefresh = false)
    }

    fun onUiEvent(event: PreferredNodeUiEvent) {
        when (event) {
            PreferredNodeUiEvent.BackClicked -> navigator.popBackStack()
            PreferredNodeUiEvent.Refresh -> load(forceRefresh = true)
            PreferredNodeUiEvent.AutomaticSelected -> choose(nodeId = null)
            PreferredNodeUiEvent.QueryCleared -> query.value = ""
            is PreferredNodeUiEvent.QueryChanged -> query.value = event.query
            is PreferredNodeUiEvent.NodeSelected -> choose(nodeId = event.nodeId)
        }
    }

    private fun load(forceRefresh: Boolean) {
        loadState.update { it.copy(isLoading = true, failed = false) }
        viewModelScope.launch {
            getGatewayNodes(forceRefresh)
                .onSuccess { nodes -> loadState.value = NodeLoadState(nodes = nodes, isLoading = false) }
                .onFailure { error ->
                    // Offline is expected here; the screen shows it instead of a global error.
                    logger.warn { "Gateway nodes could not be loaded: ${error.message}" }
                    loadState.update { it.copy(isLoading = false, failed = true) }
                }
        }
    }

    private fun choose(nodeId: String?) {
        viewModelScope.launch {
            setPreferredNode(nodeId).onFailure { error ->
                SparrowLog.error("PreferredNodeViewModel", "Preferred node could not be saved", error)
            }
        }
    }

    private data class NodeLoadState(
        val nodes: List<GatewayNode> = emptyList(),
        val isLoading: Boolean = true,
        val failed: Boolean = false
    )

    private companion object {
        const val STOP_TIMEOUT_MILLISECONDS = 5_000L
    }
}

private fun GatewayNode.toUiModel(preferredNodeId: String?): GatewayNodeUiModel =
    GatewayNodeUiModel(
        nodeId = nodeId,
        shortNodeId = nodeId.take(SHORT_NODE_ID_LENGTH),
        address = address,
        activeConnections = activeConnections,
        isPreferred = nodeId == preferredNodeId
    )

private const val SHORT_NODE_ID_LENGTH = 12
