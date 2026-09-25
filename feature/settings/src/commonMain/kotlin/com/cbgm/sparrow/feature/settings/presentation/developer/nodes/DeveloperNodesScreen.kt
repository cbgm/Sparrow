package com.cbgm.sparrow.feature.settings.presentation.developer.nodes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import com.cbgm.sparrow.core.transport.TransportDiagnosticConnectionState
import com.cbgm.sparrow.core.transport.TransportDiagnostics
import com.cbgm.sparrow.core.transport.TransportNodeDiagnostic
import com.cbgm.sparrow.core.transport.TransportNodeDiagnosticState
import com.cbgm.sparrow.core.ui.component.SparrowLazyScaffold
import com.cbgm.sparrow.core.ui.theme.SparrowTheme
import com.cbgm.sparrow.core.ui.theme.spacing
import com.cbgm.sparrow.feature.settings.presentation.developer.components.NodeDiagnosticRow
import com.cbgm.sparrow.resources.Res
import com.cbgm.sparrow.resources.feature_settings_network_no_nodes
import com.cbgm.sparrow.resources.feature_settings_network_nodes
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeveloperNodesScreen(
    diagnostics: TransportDiagnostics,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    SparrowLazyScaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { containerColor ->
            TopBar(containerColor = containerColor, onBack = onBack)
        }
    ) { innerPadding, listState ->
        val nodes = diagnostics.availableNodes
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = MaterialTheme.spacing.screenPadding,
                end = MaterialTheme.spacing.screenPadding,
                top = innerPadding.calculateTopPadding() + MaterialTheme.spacing.small,
                bottom = innerPadding.calculateBottomPadding() + MaterialTheme.spacing.medium
            ),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.base)
        ) {
            if (nodes.isEmpty()) {
                item(key = "no-nodes") {
                    Text(
                        text = stringResource(Res.string.feature_settings_network_no_nodes),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = MaterialTheme.spacing.medium),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                itemsIndexed(
                    items = nodes,
                    key = { index, node -> "${node.nodeId}:${node.websocketUrl}:$index" }
                ) { _, node ->
                    NodeDiagnosticRow(node = node, outlined = true)
                }
            }
        }
    }
}

@Composable
private fun TopBar(containerColor: Color, onBack: () -> Unit) {
    TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = containerColor,
            scrolledContainerColor = containerColor,
            titleContentColor = MaterialTheme.colorScheme.onBackground,
            navigationIconContentColor = MaterialTheme.colorScheme.onBackground
        ),
        title = {
            Text(
                text = stringResource(Res.string.feature_settings_network_nodes),
                style = MaterialTheme.typography.titleSmall
            )
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
            }
        }
    )
}

@Preview
@Composable
private fun DeveloperNodesScreenPreview() {
    SparrowTheme {
        DeveloperNodesScreen(
            diagnostics = TransportDiagnostics(
                connectionState = TransportDiagnosticConnectionState.CONNECTED,
                currentNodeId = "node-1",
                availableNodes = listOf(
                    TransportNodeDiagnostic(
                        nodeId = "node-1",
                        websocketUrl = "wss://node-a.example.test/v1/gateway",
                        state = TransportNodeDiagnosticState.CURRENT
                    ),
                    TransportNodeDiagnostic(
                        nodeId = "node-2",
                        websocketUrl = "wss://node-b.example.test/v1/gateway",
                        state = TransportNodeDiagnosticState.AVAILABLE
                    )
                )
            ),
            onBack = {}
        )
    }
}
