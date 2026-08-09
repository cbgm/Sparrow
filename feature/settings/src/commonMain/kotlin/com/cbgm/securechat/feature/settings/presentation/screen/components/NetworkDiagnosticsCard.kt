package com.cbgm.securechat.feature.settings.presentation.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.cbgm.securechat.core.time.SystemClock
import com.cbgm.securechat.core.transport.TransportDiagnosticConnectionState
import com.cbgm.securechat.core.transport.TransportDiagnostics
import com.cbgm.securechat.core.transport.TransportNodeDiagnostic
import com.cbgm.securechat.core.transport.TransportNodeDiagnosticState
import com.cbgm.securechat.core.ui.component.SecureChatCardNoAnimation
import com.cbgm.securechat.core.ui.theme.SecureChatTheme
import com.cbgm.securechat.core.ui.theme.spacing
import com.cbgm.securechat.resources.Res
import com.cbgm.securechat.resources.base_unknown
import com.cbgm.securechat.resources.feature_settings_connection_status
import com.cbgm.securechat.resources.feature_settings_current_node
import com.cbgm.securechat.resources.feature_settings_failover_count
import com.cbgm.securechat.resources.feature_settings_last_disconnect
import com.cbgm.securechat.resources.feature_settings_last_failed_node
import com.cbgm.securechat.resources.feature_settings_network_active_connections
import com.cbgm.securechat.resources.feature_settings_network_diagnostics
import com.cbgm.securechat.resources.feature_settings_network_no_nodes
import com.cbgm.securechat.resources.feature_settings_network_nodes
import com.cbgm.securechat.resources.feature_settings_node_available
import com.cbgm.securechat.resources.feature_settings_node_cooldown
import com.cbgm.securechat.resources.feature_settings_node_cooldown_remaining
import com.cbgm.securechat.resources.feature_settings_node_current
import com.cbgm.securechat.resources.feature_settings_registry_authority
import com.cbgm.securechat.resources.feature_settings_registry_not_configured
import com.cbgm.securechat.resources.feature_settings_registry_pending
import com.cbgm.securechat.resources.feature_settings_registry_url
import com.cbgm.securechat.resources.feature_settings_registry_verified
import com.cbgm.securechat.resources.feature_settings_status_connected
import com.cbgm.securechat.resources.feature_settings_status_connecting
import com.cbgm.securechat.resources.feature_settings_status_disconnected
import com.cbgm.securechat.resources.feature_settings_status_failed
import com.cbgm.securechat.resources.feature_settings_websocket_endpoint
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Duration.Companion.seconds

@Composable
internal fun NetworkDiagnosticsCard(diagnostics: TransportDiagnostics) {
    SecureChatCardNoAnimation {
        Column(
            modifier = Modifier.padding(MaterialTheme.spacing.small)
        ) {
            Text(
                text = stringResource(Res.string.feature_settings_network_diagnostics),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.size(MaterialTheme.spacing.base))

            DiagnosticRow(
                label = stringResource(Res.string.feature_settings_connection_status),
                value = diagnostics.connectionState.displayText()
            )
            DiagnosticRow(
                label = stringResource(Res.string.feature_settings_current_node),
                value = diagnostics.currentNodeId ?: stringResource(Res.string.base_unknown)
            )
            DiagnosticRow(
                label = stringResource(Res.string.feature_settings_websocket_endpoint),
                value = diagnostics.currentWebSocketUrl ?: stringResource(Res.string.base_unknown)
            )
            DiagnosticRow(
                label = stringResource(Res.string.feature_settings_registry_url),
                value =
                    diagnostics.registryUrl
                        ?: stringResource(Res.string.feature_settings_registry_not_configured)
            )
            DiagnosticRow(
                label = stringResource(Res.string.feature_settings_registry_authority),
                value = diagnostics.registryStatusText()
            )
            DiagnosticRow(
                label = stringResource(Res.string.feature_settings_failover_count),
                value = diagnostics.failoverCount.toString()
            )
            DiagnosticRow(
                label = stringResource(Res.string.feature_settings_last_failed_node),
                value = diagnostics.lastFailedNodeId ?: stringResource(Res.string.base_unknown)
            )
            DiagnosticRow(
                label = stringResource(Res.string.feature_settings_last_disconnect),
                value = diagnostics.lastDisconnectReason ?: stringResource(Res.string.base_unknown)
            )

            Spacer(modifier = Modifier.size(MaterialTheme.spacing.base))
            NodeList(diagnostics = diagnostics)
        }
    }
}

@Composable
private fun NodeList(diagnostics: TransportDiagnostics) {
    Text(
        text = stringResource(Res.string.feature_settings_network_nodes),
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
    )

    if (diagnostics.availableNodes.isEmpty()) {
        Text(
            text = stringResource(Res.string.feature_settings_network_no_nodes),
            modifier = Modifier.padding(top = MaterialTheme.spacing.base),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
        )
    } else {
        diagnostics.availableNodes.forEach { node ->
            NodeDiagnosticRow(node = node)
        }
    }
}

@Composable
private fun NodeDiagnosticRow(node: TransportNodeDiagnostic) {
    val cooldown = rememberNodeCooldown(node)
    val effectiveState =
        if (node.state == TransportNodeDiagnosticState.COOLDOWN && cooldown.remainingSeconds == 0L) {
            TransportNodeDiagnosticState.AVAILABLE
        } else {
            node.state
        }

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = MaterialTheme.spacing.base)
    ) {
        NodeDiagnosticHeader(
            nodeId = node.nodeId,
            state = effectiveState,
            activeConnections = node.activeConnections,
            cooldownRemainingSeconds = cooldown.remainingSeconds
        )

        Text(
            text = node.websocketUrl,
            modifier = Modifier.padding(top = MaterialTheme.spacing.base / 2),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
        )
    }
}

@Composable
private fun NodeDiagnosticHeader(
    nodeId: String,
    state: TransportNodeDiagnosticState,
    activeConnections: Int,
    cooldownRemainingSeconds: Long
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = nodeId,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground
        )

        Column(
            modifier = Modifier.padding(start = MaterialTheme.spacing.small),
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = state.displayText(cooldownRemainingSeconds),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = state.displayColor()
            )
            Text(
                text =
                    stringResource(
                        Res.string.feature_settings_network_active_connections,
                        activeConnections
                    ),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
private fun rememberNodeCooldown(node: TransportNodeDiagnostic): NodeCooldown {
    val cooldownUntil = node.cooldownUntilEpochMilliseconds
    var nowEpochMilliseconds by remember(cooldownUntil) {
        mutableLongStateOf(SystemClock.nowEpochMilliseconds())
    }

    LaunchedEffect(cooldownUntil) {
        while (cooldownUntil != null && nowEpochMilliseconds < cooldownUntil) {
            delay(1.seconds)
            nowEpochMilliseconds = SystemClock.nowEpochMilliseconds()
        }
    }

    val remainingMilliseconds =
        cooldownUntil
            ?.minus(nowEpochMilliseconds)
            ?.coerceAtLeast(0L)
            ?: 0L

    return NodeCooldown(
        remainingSeconds = (remainingMilliseconds + 999L) / 1_000L
    )
}

private data class NodeCooldown(
    val remainingSeconds: Long
)

@Composable
private fun DiagnosticRow(
    label: String,
    value: String
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = MaterialTheme.spacing.base / 2)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
        )

        Text(
            text = value,
            modifier = Modifier.padding(top = MaterialTheme.spacing.base / 2),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
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

@Composable
private fun TransportNodeDiagnosticState.displayText(
    cooldownRemainingSeconds: Long = 0L
): String =
    when (this) {
        TransportNodeDiagnosticState.CURRENT ->
            stringResource(Res.string.feature_settings_node_current)

        TransportNodeDiagnosticState.AVAILABLE ->
            stringResource(Res.string.feature_settings_node_available)

        TransportNodeDiagnosticState.COOLDOWN ->
            if (cooldownRemainingSeconds > 0L) {
                stringResource(
                    Res.string.feature_settings_node_cooldown_remaining,
                    cooldownRemainingSeconds
                )
            } else {
                stringResource(Res.string.feature_settings_node_cooldown)
            }
    }

@Composable
private fun TransportNodeDiagnosticState.displayColor(): Color =
    when (this) {
        TransportNodeDiagnosticState.CURRENT -> MaterialTheme.colorScheme.secondary
        TransportNodeDiagnosticState.AVAILABLE -> MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
        TransportNodeDiagnosticState.COOLDOWN -> MaterialTheme.colorScheme.error
    }

@Preview
@Composable
fun NetworkDiagnosticCardPreview() {
    SecureChatTheme {
        NetworkDiagnosticsCard(
            diagnostics =
                TransportDiagnostics(
                    connectionState = TransportDiagnosticConnectionState.CONNECTED,
                    currentNodeId = "1dc6103605070c67",
                    currentWebSocketUrl = "ws://192.168.178.60:8490/relay",
                    registryUrl = "http://10.0.2.2:8390",
                    registryAuthorityVerified = true,
                    availableNodes =
                        listOf(
                            TransportNodeDiagnostic(
                                nodeId = "1dc6103605070c67",
                                websocketUrl = "ws://192.168.178.60:8490/relay",
                                state = TransportNodeDiagnosticState.CURRENT
                            ),
                            TransportNodeDiagnostic(
                                nodeId = "901d125ea367d974",
                                websocketUrl = "ws://192.168.178.21:8490/relay",
                                state = TransportNodeDiagnosticState.AVAILABLE
                            ),
                            TransportNodeDiagnostic(
                                nodeId = "901d125ea367d974",
                                websocketUrl = "ws://192.168.178.21:8490/relay",
                                state = TransportNodeDiagnosticState.COOLDOWN
                            )
                        )
                )
        )
    }
}
