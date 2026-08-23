package com.cbgm.sparrow.feature.settings.presentation.developer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.cbgm.sparrow.core.transport.TransportDiagnosticConnectionState
import com.cbgm.sparrow.core.transport.TransportDiagnostics
import com.cbgm.sparrow.core.transport.TransportNodeDiagnostic
import com.cbgm.sparrow.core.transport.TransportNodeDiagnosticState
import com.cbgm.sparrow.core.ui.component.SparrowCardNoAnimation
import com.cbgm.sparrow.core.ui.component.SparrowDestructiveButton
import com.cbgm.sparrow.core.ui.component.SparrowScrollScaffold
import com.cbgm.sparrow.core.ui.theme.Dimens
import com.cbgm.sparrow.core.ui.theme.SparrowTheme
import com.cbgm.sparrow.core.ui.theme.spacing
import com.cbgm.sparrow.feature.settings.domain.model.BuildInfo
import com.cbgm.sparrow.feature.settings.presentation.developer.components.NetworkDiagnosticsCard
import com.cbgm.sparrow.feature.settings.presentation.developer.model.DeveloperMenuUiEvent
import com.cbgm.sparrow.feature.settings.presentation.developer.model.DeveloperMenuUiState
import com.cbgm.sparrow.resources.Res
import com.cbgm.sparrow.resources.base_build_type
import com.cbgm.sparrow.resources.base_clear_local_data
import com.cbgm.sparrow.resources.base_git_sha
import com.cbgm.sparrow.resources.base_unknown
import com.cbgm.sparrow.resources.base_version_code
import com.cbgm.sparrow.resources.base_version_name
import com.cbgm.sparrow.resources.feature_settings_build_info
import com.cbgm.sparrow.resources.feature_settings_danger_zone
import com.cbgm.sparrow.resources.feature_settings_danger_zone_description
import com.cbgm.sparrow.resources.feature_settings_developer_menu
import com.cbgm.sparrow.resources.feature_settings_disable_developer_mode
import org.jetbrains.compose.resources.stringResource

@Composable
fun DeveloperMenuScreen(
    uiState: DeveloperMenuUiState,
    onUiEvent: (DeveloperMenuUiEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    SparrowScrollScaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { containerColor ->
            TopBar(
                containerColor = containerColor,
                onBack = { onUiEvent(DeveloperMenuUiEvent.BackClicked) }
            )
        }
    ) { innerPadding, scrollState ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(
                        top = innerPadding.calculateTopPadding(),
                        bottom = innerPadding.calculateBottomPadding(),
                        start = MaterialTheme.spacing.medium,
                        end = MaterialTheme.spacing.medium
                    ),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
        ) {
            BuildInfoCard(buildInfo = uiState.buildInfo)

            NetworkDiagnosticsCard(diagnostics = uiState.transportDiagnostics)

            DangerZoneCard(
                isClearingLocalData = uiState.isClearingLocalData,
                onClearLocalData = { onUiEvent(DeveloperMenuUiEvent.ClearLocalDataClicked) },
                onDisableDeveloperMode = { onUiEvent(DeveloperMenuUiEvent.DisableDeveloperModeClicked) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TopBar(
    containerColor: Color,
    onBack: () -> Unit
) {
    TopAppBar(
        colors =
            TopAppBarDefaults.topAppBarColors(
                containerColor = containerColor,
                scrolledContainerColor = containerColor,
                titleContentColor = MaterialTheme.colorScheme.onBackground,
                navigationIconContentColor = MaterialTheme.colorScheme.onBackground
            ),
        title = {
            Text(
                text = stringResource(Res.string.feature_settings_developer_menu),
                style = MaterialTheme.typography.titleSmall
            )
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = null
                )
            }
        }
    )
}

@Composable
private fun BuildInfoCard(buildInfo: BuildInfo) {
    SparrowCardNoAnimation {
        Column(
            modifier = Modifier.padding(MaterialTheme.spacing.small)
        ) {
            Text(
                text = stringResource(Res.string.feature_settings_build_info),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.size(MaterialTheme.spacing.base))

            BuildInfoRow(
                label = stringResource(Res.string.base_version_name),
                value = buildInfo.versionName
            )

            BuildInfoRow(
                label = stringResource(Res.string.base_version_code),
                value = buildInfo.versionCode.toString()
            )

            BuildInfoRow(
                label = stringResource(Res.string.base_build_type),
                value = buildInfo.buildType
            )

            BuildInfoRow(
                label = stringResource(Res.string.base_git_sha),
                value = buildInfo.gitSha ?: stringResource(Res.string.base_unknown)
            )
        }
    }
}

@Composable
private fun DangerZoneCard(
    isClearingLocalData: Boolean,
    onClearLocalData: () -> Unit,
    onDisableDeveloperMode: () -> Unit
) {
    SparrowCardNoAnimation {
        Column(
            modifier = Modifier.padding(MaterialTheme.spacing.small)
        ) {
            Text(
                text = stringResource(Res.string.feature_settings_danger_zone),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.error
            )

            Text(
                text = stringResource(Res.string.feature_settings_danger_zone_description),
                modifier =
                    Modifier.padding(
                        top = MaterialTheme.spacing.base.div(2),
                        bottom = MaterialTheme.spacing.small
                    ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            SparrowDestructiveButton(
                onClick = onClearLocalData,
                enabled = !isClearingLocalData,
                modifier = Modifier.fillMaxWidth(),
                content = {
                    if (isClearingLocalData) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(Dimens.DeveloperMenuScreen.progressSize),
                            strokeWidth = Dimens.Base.progressIndicatorStrokeWidth,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    } else {
                        Text(
                            text = stringResource(Res.string.base_clear_local_data),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            )

            Spacer(modifier = Modifier.size(MaterialTheme.spacing.base))

            OutlinedButton(
                onClick = onDisableDeveloperMode,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = stringResource(Res.string.feature_settings_disable_developer_mode))
            }
        }
    }
}

@Composable
private fun BuildInfoRow(
    label: String,
    value: String
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = MaterialTheme.spacing.base / 2),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Preview
@Composable
fun DeveloperScreenPreview() {
    SparrowTheme {
        DeveloperMenuScreen(
            uiState =
                DeveloperMenuUiState(
                    buildInfo =
                        BuildInfo(
                            versionName = "1.0.0",
                            versionCode = 1,
                            buildType = "debug",
                            gitSha = null
                        ),
                    transportDiagnostics =
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
                                    )
                                )
                        )
                ),
            onUiEvent = {}
        )
    }
}
