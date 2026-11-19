package com.cbgm.sparrow.feature.settings.presentation.developer.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.cbgm.sparrow.core.ui.component.SparrowDetailRow
import com.cbgm.sparrow.core.ui.theme.SparrowTheme
import com.cbgm.sparrow.core.ui.theme.spacing
import com.cbgm.sparrow.feature.transport.TransportDiagnosticConnectionState
import com.cbgm.sparrow.feature.transport.TransportDiagnostics
import com.cbgm.sparrow.feature.transport.TransportNodeDiagnostic
import com.cbgm.sparrow.feature.transport.TransportNodeDiagnosticState
import com.cbgm.sparrow.resources.Res
import com.cbgm.sparrow.resources.base_unknown
import com.cbgm.sparrow.resources.feature_settings_connection_status
import com.cbgm.sparrow.resources.feature_settings_current_node
import com.cbgm.sparrow.resources.feature_settings_failover_count
import com.cbgm.sparrow.resources.feature_settings_last_disconnect
import com.cbgm.sparrow.resources.feature_settings_last_failed_node
import com.cbgm.sparrow.resources.feature_settings_network_diagnostics
import com.cbgm.sparrow.resources.feature_settings_network_nodes
import com.cbgm.sparrow.resources.feature_settings_network_show_all_nodes
import com.cbgm.sparrow.resources.feature_settings_registry_authority
import com.cbgm.sparrow.resources.feature_settings_registry_not_configured
import com.cbgm.sparrow.resources.feature_settings_registry_pending
import com.cbgm.sparrow.resources.feature_settings_registry_url
import com.cbgm.sparrow.resources.feature_settings_registry_verified
import com.cbgm.sparrow.resources.feature_settings_status_connected
import com.cbgm.sparrow.resources.feature_settings_status_connecting
import com.cbgm.sparrow.resources.feature_settings_status_disconnected
import com.cbgm.sparrow.resources.feature_settings_status_failed
import com.cbgm.sparrow.resources.feature_settings_websocket_endpoint
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun NetworkDiagnosticsCard(
    diagnostics: TransportDiagnostics,
    onShowAllNodes: () -> Unit
) {
    Column(
        modifier = Modifier.padding(vertical = MaterialTheme.spacing.medium)
    ) {
        SettingsSection(
            title = stringResource(Res.string.feature_settings_network_diagnostics)
        ) {
            SparrowDetailRow(
                title = stringResource(Res.string.feature_settings_connection_status),
                subtitle = diagnostics.connectionState.displayText(),
                showChevron = false,
                showDivider = true
            )
            SparrowDetailRow(
                title = stringResource(Res.string.feature_settings_current_node),
                subtitle = diagnostics.currentNodeId ?: stringResource(Res.string.base_unknown),
                showChevron = false,
                showDivider = true
            )
            SparrowDetailRow(
                title = stringResource(Res.string.feature_settings_websocket_endpoint),
                subtitle = diagnostics.currentWebSocketUrl ?: stringResource(Res.string.base_unknown),
                showChevron = false,
                showDivider = true
            )
            SparrowDetailRow(
                title = stringResource(Res.string.feature_settings_registry_url),
                subtitle =
                    diagnostics.registryUrl
                        ?: stringResource(Res.string.feature_settings_registry_not_configured),
                showChevron = false,
                showDivider = true
            )
            SparrowDetailRow(
                title = stringResource(Res.string.feature_settings_registry_authority),
                subtitle = diagnostics.registryStatusText(),
                showChevron = false,
                showDivider = true
            )
            SparrowDetailRow(
                title = stringResource(Res.string.feature_settings_failover_count),
                subtitle = diagnostics.failoverCount.toString(),
                showChevron = false,
                showDivider = true
            )
            SparrowDetailRow(
                title = stringResource(Res.string.feature_settings_last_failed_node),
                subtitle = diagnostics.lastFailedNodeId ?: stringResource(Res.string.base_unknown),
                showChevron = false,
                showDivider = true
            )
            SparrowDetailRow(
                title = stringResource(Res.string.feature_settings_last_disconnect),
                subtitle = diagnostics.lastDisconnectReason ?: stringResource(Res.string.base_unknown),
                showChevron = false,
                showDivider = true
            )
            SparrowDetailRow(
                icon = null,
                title = stringResource(Res.string.feature_settings_network_nodes),
                subtitle = stringResource(
                    Res.string.feature_settings_network_show_all_nodes,
                    diagnostics.availableNodes.size
                ),
                onClick = onShowAllNodes,
                showDivider = true
            )
        }
    }
}

@Composable
private fun TransportDiagnosticConnectionState.displayText(): String =
    when (this) {
        TransportDiagnosticConnectionState.DISCONNECTED ->
            stringResource(Res.string.feature_settings_status_disconnected)

        TransportDiagnosticConnectionState.CONNECTING ->
            stringResource(Res.string.feature_settings_status_connecting)

        TransportDiagnosticConnectionState.CONNECTED ->
            stringResource(Res.string.feature_settings_status_connected)

        TransportDiagnosticConnectionState.FAILED ->
            stringResource(Res.string.feature_settings_status_failed)
    }

@Composable
private fun TransportDiagnostics.registryStatusText(): String =
    when {
        registryUrl == null -> stringResource(Res.string.feature_settings_registry_not_configured)
        registryAuthorityVerified == true -> stringResource(Res.string.feature_settings_registry_verified)
        registryAuthorityVerified == false -> stringResource(Res.string.feature_settings_status_failed)
        else -> stringResource(Res.string.feature_settings_registry_pending)
    }

@Preview
@Composable
fun NetworkDiagnosticCardPreview() {
    SparrowTheme {
        NetworkDiagnosticsCard(
            onShowAllNodes = {},
            diagnostics =
                TransportDiagnostics(
                    connectionState = TransportDiagnosticConnectionState.CONNECTED,
                    currentNodeId = "1dc6103605070c67",
                    currentWebSocketUrl = "wss://node-a.example.test/v1/gateway",
                    registryUrl = "https://plane.example.test",
                    registryAuthorityVerified = true,
                    availableNodes =
                        listOf(
                            TransportNodeDiagnostic(
                                nodeId = "1dc6103605070c67",
                                websocketUrl = "wss://node-a.example.test/v1/gateway",
                                state = TransportNodeDiagnosticState.CURRENT
                            ),
                            TransportNodeDiagnostic(
                                nodeId = "901d125ea367d974",
                                websocketUrl = "wss://node-b.example.test/v1/gateway",
                                state = TransportNodeDiagnosticState.AVAILABLE
                            ),
                            TransportNodeDiagnostic(
                                nodeId = "901d125ea367d974",
                                websocketUrl = "wss://node-b.example.test/v1/gateway",
                                state = TransportNodeDiagnosticState.COOLDOWN
                            )
                        )
                )
        )
    }
}
