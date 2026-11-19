package com.cbgm.sparrow.feature.settings.presentation.developer.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.cbgm.sparrow.core.ui.theme.Alpha
import com.cbgm.sparrow.core.ui.theme.Dimens
import com.cbgm.sparrow.core.ui.theme.SparrowTheme
import com.cbgm.sparrow.core.ui.theme.spacing
import com.cbgm.sparrow.core.util.time.SystemClock
import com.cbgm.sparrow.feature.transport.TransportNodeDiagnostic
import com.cbgm.sparrow.feature.transport.TransportNodeDiagnosticState
import com.cbgm.sparrow.resources.Res
import com.cbgm.sparrow.resources.feature_settings_node_available
import com.cbgm.sparrow.resources.feature_settings_node_cooldown
import com.cbgm.sparrow.resources.feature_settings_node_cooldown_remaining
import com.cbgm.sparrow.resources.feature_settings_node_current
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Duration.Companion.seconds

@Composable
internal fun NodeDiagnosticRow(
    node: TransportNodeDiagnostic
) {
    val cooldown = rememberNodeCooldown(node)
    val effectiveState =
        if (node.state == TransportNodeDiagnosticState.COOLDOWN && cooldown.remainingSeconds == 0L) {
            TransportNodeDiagnosticState.AVAILABLE
        } else {
            node.state
        }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(Dimens.SettingsScreen.primaryIconSize),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier.size(Dimens.ControlPlaneSettingsScreen.statusIndicatorSize)
                    .background(node.state.displayColor(), CircleShape)
            )
        }

        Column(modifier = Modifier.padding(MaterialTheme.spacing.small)) {
            NodeDiagnosticInfo(
                nodeId = node.nodeId,
                state = effectiveState,
                address = node.websocketUrl,
                cooldownRemainingSeconds = cooldown.remainingSeconds
            )
        }
    }
}

@Composable
private fun NodeDiagnosticInfo(
    nodeId: String,
    address: String,
    state: TransportNodeDiagnosticState,
    cooldownRemainingSeconds: Long
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.micro)
    ) {
        Text(
            text = nodeId,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = state.displayText(cooldownRemainingSeconds),
            style = MaterialTheme.typography.bodySmall,
            color = state.displayColor()
        )
        Text(
            text = address,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = Alpha.NetworkDiagnosticsCard.available)
        )
    }
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
        TransportNodeDiagnosticState.CURRENT -> MaterialTheme.colorScheme.primary
        TransportNodeDiagnosticState.AVAILABLE -> MaterialTheme.colorScheme.tertiary
        TransportNodeDiagnosticState.COOLDOWN -> MaterialTheme.colorScheme.error
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

@Preview
@Composable
private fun NodeDiagnosticRowPreview() {
    SparrowTheme {
        NodeDiagnosticRow(
            node = TransportNodeDiagnostic(
                nodeId = "1dc6103605070c67",
                websocketUrl = "wss://node-a.example.test/v1/gateway",
                state = TransportNodeDiagnosticState.CURRENT
            )
        )
    }
}
