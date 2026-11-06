package com.cbgm.sparrow.feature.settings.presentation.network.circuit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.cbgm.sparrow.core.ui.component.SparrowCardNoAnimation
import com.cbgm.sparrow.core.ui.component.SparrowLazyScaffold
import com.cbgm.sparrow.core.ui.theme.SparrowTheme
import com.cbgm.sparrow.core.ui.theme.spacing
import com.cbgm.sparrow.feature.settings.presentation.network.circuit.model.CircuitHopsUiEvent
import com.cbgm.sparrow.feature.settings.presentation.network.circuit.model.CircuitHopsUiState
import com.cbgm.sparrow.feature.settings.presentation.network.circuit.model.CircuitUiStatus
import com.cbgm.sparrow.resources.Res
import com.cbgm.sparrow.resources.feature_settings_circuit
import com.cbgm.sparrow.resources.feature_settings_circuit_estimate
import com.cbgm.sparrow.resources.feature_settings_circuit_fewer
import com.cbgm.sparrow.resources.feature_settings_circuit_hint
import com.cbgm.sparrow.resources.feature_settings_circuit_hops
import com.cbgm.sparrow.resources.feature_settings_circuit_send_directly
import com.cbgm.sparrow.resources.feature_settings_circuit_send_directly_off
import com.cbgm.sparrow.resources.feature_settings_circuit_send_directly_on
import com.cbgm.sparrow.resources.feature_settings_circuit_status_building
import com.cbgm.sparrow.resources.feature_settings_circuit_status_connected
import com.cbgm.sparrow.resources.feature_settings_circuit_status_unavailable
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import kotlin.math.roundToInt

@Composable
fun CircuitHopsScreen(
    uiState: CircuitHopsUiState,
    onUiEvent: (CircuitHopsUiEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    SparrowLazyScaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { containerColor ->
            CircuitHopsTopBar(containerColor = containerColor, onBack = { onUiEvent(CircuitHopsUiEvent.BackClicked) })
        }
    ) { innerPadding, listState ->
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
                    text = stringResource(Res.string.feature_settings_circuit_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            item(key = "hops") {
                SparrowCardNoAnimation {
                    HopCountChooser(uiState = uiState, onUiEvent = onUiEvent)
                }
            }
            item(key = "fallback") {
                SparrowCardNoAnimation {
                    FallbackChooser(uiState = uiState, onUiEvent = onUiEvent)
                }
            }
        }
    }
}

@Composable
private fun HopCountChooser(
    uiState: CircuitHopsUiState,
    onUiEvent: (CircuitHopsUiEvent) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(MaterialTheme.spacing.base),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
    ) {
        Text(
            text = stringResource(Res.string.feature_settings_circuit_hops, uiState.selectedHopCount),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Slider(
            value = uiState.selectedHopCount.toFloat(),
            onValueChange = { value -> onUiEvent(CircuitHopsUiEvent.HopCountChanged(value.roundToInt())) },
            onValueChangeFinished = { onUiEvent(CircuitHopsUiEvent.HopCountChosen) },
            valueRange = uiState.minimumHopCount.toFloat()..uiState.maximumHopCount.toFloat(),
            steps = uiState.maximumHopCount - uiState.minimumHopCount - 1
        )
        StatusLine(text = statusText(uiState.status))
        uiState.estimatedLatencyMilliseconds?.let { estimate ->
            StatusLine(text = stringResource(Res.string.feature_settings_circuit_estimate, estimate.toInt(), uiState.selectedHopCount))
        }
        (uiState.status as? CircuitUiStatus.Connected)
            ?.takeIf { connected -> connected.hopCount < connected.requestedHopCount }
            ?.let { connected ->
                StatusLine(
                    text = stringResource(Res.string.feature_settings_circuit_fewer, connected.hopCount, connected.requestedHopCount),
                    color = MaterialTheme.colorScheme.error
                )
            }
    }
}

@Composable
private fun FallbackChooser(
    uiState: CircuitHopsUiState,
    onUiEvent: (CircuitHopsUiEvent) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(MaterialTheme.spacing.base),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.base)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(Res.string.feature_settings_circuit_send_directly),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            StatusLine(text = stringResource(fallbackExplanation(uiState.sendsDirectlyWithoutCircuit)))
        }
        Switch(
            checked = uiState.sendsDirectlyWithoutCircuit,
            onCheckedChange = { checked -> onUiEvent(CircuitHopsUiEvent.SendDirectlyChanged(checked)) }
        )
    }
}

private fun fallbackExplanation(sendsDirectly: Boolean): StringResource =
    if (sendsDirectly) {
        Res.string.feature_settings_circuit_send_directly_on
    } else {
        Res.string.feature_settings_circuit_send_directly_off
    }

@Composable
private fun statusText(status: CircuitUiStatus): String =
    when (status) {
        CircuitUiStatus.Building -> stringResource(Res.string.feature_settings_circuit_status_building)
        CircuitUiStatus.Unavailable -> stringResource(Res.string.feature_settings_circuit_status_unavailable)
        is CircuitUiStatus.Connected ->
            stringResource(Res.string.feature_settings_circuit_status_connected, status.hopCount, status.latencyMilliseconds.toInt())
    }

@Composable
private fun StatusLine(
    text: String,
    color: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    Text(text = text, style = MaterialTheme.typography.bodySmall, color = color)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CircuitHopsTopBar(
    containerColor: Color,
    onBack: () -> Unit
) {
    CenterAlignedTopAppBar(
        title = {
            Text(text = stringResource(Res.string.feature_settings_circuit), style = MaterialTheme.typography.titleSmall)
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = containerColor,
            scrolledContainerColor = containerColor,
            titleContentColor = MaterialTheme.colorScheme.onBackground,
            navigationIconContentColor = MaterialTheme.colorScheme.onBackground
        )
    )
}

@Preview
@Composable
private fun CircuitHopsScreenPreview() {
    SparrowTheme {
        CircuitHopsScreen(
            uiState = CircuitHopsUiState(
                selectedHopCount = 3,
                status = CircuitUiStatus.Connected(hopCount = 2, requestedHopCount = 3, latencyMilliseconds = 180),
                estimatedLatencyMilliseconds = 270
            ),
            onUiEvent = {}
        )
    }
}
