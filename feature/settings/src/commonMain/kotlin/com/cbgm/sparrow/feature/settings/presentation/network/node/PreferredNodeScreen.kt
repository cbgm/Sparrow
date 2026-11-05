package com.cbgm.sparrow.feature.settings.presentation.network.node

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.cbgm.sparrow.core.ui.component.SparrowCardNoAnimation
import com.cbgm.sparrow.core.ui.component.SparrowLazyScaffold
import com.cbgm.sparrow.core.ui.component.SparrowSearchField
import com.cbgm.sparrow.core.ui.theme.SparrowTheme
import com.cbgm.sparrow.core.ui.theme.spacing
import com.cbgm.sparrow.feature.settings.presentation.network.node.model.GatewayNodeUiModel
import com.cbgm.sparrow.feature.settings.presentation.network.node.model.PreferredNodeUiEvent
import com.cbgm.sparrow.feature.settings.presentation.network.node.model.PreferredNodeUiState
import com.cbgm.sparrow.resources.Res
import com.cbgm.sparrow.resources.feature_settings_control_planes_refresh
import com.cbgm.sparrow.resources.feature_settings_preferred_node
import com.cbgm.sparrow.resources.feature_settings_preferred_node_automatic
import com.cbgm.sparrow.resources.feature_settings_preferred_node_automatic_description
import com.cbgm.sparrow.resources.feature_settings_preferred_node_connections
import com.cbgm.sparrow.resources.feature_settings_preferred_node_hint
import com.cbgm.sparrow.resources.feature_settings_preferred_node_load_failed
import com.cbgm.sparrow.resources.feature_settings_preferred_node_none_found
import com.cbgm.sparrow.resources.feature_settings_preferred_node_search
import com.cbgm.sparrow.resources.feature_settings_preferred_node_unlisted
import org.jetbrains.compose.resources.stringResource

@Composable
fun PreferredNodeScreen(
    uiState: PreferredNodeUiState,
    onUiEvent: (PreferredNodeUiEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    SparrowLazyScaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { containerColor ->
            PreferredNodeTopBar(
                containerColor = containerColor,
                onBack = { onUiEvent(PreferredNodeUiEvent.BackClicked) },
                onRefresh = { onUiEvent(PreferredNodeUiEvent.Refresh) }
            )
        }
    ) { innerPadding, listState ->
        val status = nodeListStatus(uiState)
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = listState,
            contentPadding = PaddingValues(
                start = MaterialTheme.spacing.screenPadding,
                end = MaterialTheme.spacing.screenPadding,
                top = innerPadding.calculateTopPadding() + MaterialTheme.spacing.small,
                bottom = innerPadding.calculateBottomPadding() + MaterialTheme.spacing.medium
            ),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.base)
        ) {
            item(key = "hint") {
                Text(
                    text = stringResource(Res.string.feature_settings_preferred_node_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            item(key = "automatic") {
                SparrowCardNoAnimation {
                    NodeChoiceRow(
                        title = stringResource(Res.string.feature_settings_preferred_node_automatic),
                        subtitle = stringResource(Res.string.feature_settings_preferred_node_automatic_description),
                        selected = uiState.preferredNodeId == null,
                        onClick = { onUiEvent(PreferredNodeUiEvent.AutomaticSelected) }
                    )
                }
            }

            item(key = "search") {
                SparrowSearchField(
                    searchQuery = uiState.query,
                    onSearchQueryChanged = { onUiEvent(PreferredNodeUiEvent.QueryChanged(it)) },
                    onClear = { onUiEvent(PreferredNodeUiEvent.QueryCleared) },
                    placeholder = stringResource(Res.string.feature_settings_preferred_node_search)
                )
            }

            if (uiState.isLoading) {
                item(key = "loading") { LinearProgressIndicator(modifier = Modifier.fillMaxWidth()) }
            }

            status?.let { text ->
                item(key = "status") { StatusText(text) }
            }

            items(items = uiState.nodes, key = GatewayNodeUiModel::nodeId) { node ->
                SparrowCardNoAnimation {
                    NodeChoiceRow(
                        title = node.address,
                        subtitle =
                            "${node.shortNodeId} · " +
                                stringResource(Res.string.feature_settings_preferred_node_connections, node.activeConnections),
                        selected = node.isPreferred,
                        onClick = { onUiEvent(PreferredNodeUiEvent.NodeSelected(node.nodeId)) }
                    )
                }
            }
        }
    }
}

@Composable
private fun nodeListStatus(uiState: PreferredNodeUiState): String? =
    when {
        uiState.isLoading -> null
        uiState.loadFailed -> stringResource(Res.string.feature_settings_preferred_node_load_failed)
        uiState.preferredUnlisted -> stringResource(Res.string.feature_settings_preferred_node_unlisted)
        uiState.nodes.isEmpty() -> stringResource(Res.string.feature_settings_preferred_node_none_found)
        else -> null
    }

@Composable
private fun StatusText(text: String) {
    Text(
        text = text,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = MaterialTheme.spacing.small),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun NodeChoiceRow(
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(
                    horizontal = MaterialTheme.spacing.small,
                    vertical = MaterialTheme.spacing.base
                ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PreferredNodeTopBar(
    containerColor: Color,
    onBack: () -> Unit,
    onRefresh: () -> Unit
) {
    Column {
        CenterAlignedTopAppBar(
            title = {
                Text(
                    text = stringResource(Res.string.feature_settings_preferred_node),
                    style = MaterialTheme.typography.titleSmall
                )
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                }
            },
            actions = {
                IconButton(onClick = onRefresh) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = stringResource(Res.string.feature_settings_control_planes_refresh)
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = containerColor,
                scrolledContainerColor = containerColor,
                titleContentColor = MaterialTheme.colorScheme.onBackground,
                navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
                actionIconContentColor = MaterialTheme.colorScheme.onBackground
            )
        )
    }
}

@Preview
@Composable
private fun PreferredNodeScreenPreview() {
    SparrowTheme {
        PreferredNodeScreen(
            uiState = PreferredNodeUiState(
                preferredNodeId = "node-b",
                isLoading = false,
                nodes = listOf(
                    GatewayNodeUiModel("node-b", "node-b", "gateway-80-131-151-133.sslip.io", 12, true),
                    GatewayNodeUiModel("node-a", "node-a", "node.example.org", 3, false)
                )
            ),
            onUiEvent = {}
        )
    }
}
