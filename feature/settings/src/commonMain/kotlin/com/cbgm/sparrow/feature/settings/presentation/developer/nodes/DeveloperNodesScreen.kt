package com.cbgm.sparrow.feature.settings.presentation.developer.nodes

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import com.cbgm.sparrow.core.ui.component.SparrowDetailRow
import com.cbgm.sparrow.core.ui.component.SparrowLazyScaffold
import com.cbgm.sparrow.core.ui.component.SparrowSectionLabel
import com.cbgm.sparrow.core.ui.component.SparrowTopBar
import com.cbgm.sparrow.core.ui.icon.SparrowIcons
import com.cbgm.sparrow.core.ui.theme.Dimens
import com.cbgm.sparrow.core.ui.theme.SparrowTheme
import com.cbgm.sparrow.core.ui.theme.divider
import com.cbgm.sparrow.core.ui.theme.spacing
import com.cbgm.sparrow.feature.settings.presentation.developer.components.NodeDiagnosticRow
import com.cbgm.sparrow.feature.transport.domain.model.TransportDiagnosticConnectionState
import com.cbgm.sparrow.feature.transport.domain.model.TransportDiagnostics
import com.cbgm.sparrow.feature.transport.domain.model.TransportNodeDiagnostic
import com.cbgm.sparrow.feature.transport.domain.model.TransportNodeDiagnosticState
import com.cbgm.sparrow.resources.Res
import com.cbgm.sparrow.resources.feature_settings_network_no_nodes
import com.cbgm.sparrow.resources.feature_settings_network_nodes
import com.cbgm.sparrow.resources.feature_settings_preferred_node
import com.cbgm.sparrow.resources.feature_settings_preferred_node_subtitle
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeveloperNodesScreen(
    diagnostics: TransportDiagnostics,
    onBack: () -> Unit,
    onPreferredNodeClicked: () -> Unit,
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
            )
        ) {
            item(key = "preferred-node") {
                PreferredNodeLink(onClick = onPreferredNodeClicked)
            }
            item(key = "node-label") {
                NodesLabel(
                    size = nodes.size,
                    modifier = Modifier.padding(top = MaterialTheme.spacing.medium)
                )
            }
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
                ) { index, node ->
                    NodeDiagnosticRow(node = node)
                    if (index < nodes.lastIndex) {
                        HorizontalDivider(
                            modifier = Modifier.fillMaxWidth(),
                            thickness = Dimens.Base.dividerThickness,
                            color = MaterialTheme.colorScheme.divider
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PreferredNodeLink(onClick: () -> Unit) {
    SparrowDetailRow(
        title = stringResource(Res.string.feature_settings_preferred_node),
        subtitle = stringResource(Res.string.feature_settings_preferred_node_subtitle),
        onClick = onClick
    )
}

@Composable
private fun NodesLabel(size: Int, modifier: Modifier = Modifier) {
    Row(modifier = modifier) {
        SparrowSectionLabel(
            text = stringResource(Res.string.feature_settings_network_nodes),
            modifier = Modifier.weight(1f)
        )
        Text(
            text = size.toString(),
            modifier = Modifier.padding(start = MaterialTheme.spacing.small),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.outline
        )
    }
}

@Composable
private fun TopBar(containerColor: Color, onBack: () -> Unit) {
    SparrowTopBar(
        containerColor = containerColor,
        title = {
            Text(
                text = stringResource(Res.string.feature_settings_network_nodes)
            )
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(imageVector = SparrowIcons.ArrowBack, contentDescription = null)
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
            onBack = {},
            onPreferredNodeClicked = {}
        )
    }
}
