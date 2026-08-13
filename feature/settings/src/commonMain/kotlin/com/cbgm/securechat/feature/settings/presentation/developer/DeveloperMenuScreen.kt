package com.cbgm.securechat.feature.settings.presentation.developer

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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.unit.dp
import com.cbgm.securechat.core.transport.TransportDiagnosticConnectionState
import com.cbgm.securechat.core.transport.TransportDiagnostics
import com.cbgm.securechat.core.transport.TransportNodeDiagnostic
import com.cbgm.securechat.core.transport.TransportNodeDiagnosticState
import com.cbgm.securechat.core.ui.component.SecureChatCardNoAnimation
import com.cbgm.securechat.core.ui.component.SecureChatScrollScaffold
import com.cbgm.securechat.core.ui.theme.SecureChatTheme
import com.cbgm.securechat.core.ui.theme.spacing
import com.cbgm.securechat.feature.settings.domain.model.BuildInfo
import com.cbgm.securechat.feature.settings.presentation.developer.components.NetworkDiagnosticsCard
import com.cbgm.securechat.feature.settings.presentation.developer.model.DeveloperMenuUiEvent
import com.cbgm.securechat.feature.settings.presentation.developer.model.DeveloperMenuUiState
import com.cbgm.securechat.resources.Res
import com.cbgm.securechat.resources.base_build_type
import com.cbgm.securechat.resources.base_clear_local_data
import com.cbgm.securechat.resources.base_git_sha
import com.cbgm.securechat.resources.base_unknown
import com.cbgm.securechat.resources.base_version_code
import com.cbgm.securechat.resources.base_version_name
import com.cbgm.securechat.resources.feature_settings_build_info
import com.cbgm.securechat.resources.feature_settings_danger_zone
import com.cbgm.securechat.resources.feature_settings_danger_zone_description
import com.cbgm.securechat.resources.feature_settings_developer_menu
import com.cbgm.securechat.resources.feature_settings_disable_developer_mode
import org.jetbrains.compose.resources.stringResource

@Composable
fun DeveloperMenuScreen(
    uiState: DeveloperMenuUiState,
    onUiEvent: (DeveloperMenuUiEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    SecureChatScrollScaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { containerColor ->
            DeveloperMenuTopBar(
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
private fun DeveloperMenuTopBar(
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
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
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
    SecureChatCardNoAnimation {
        Column(
            modifier = Modifier.padding(MaterialTheme.spacing.small)
        ) {
            Text(
                text = stringResource(Res.string.feature_settings_build_info),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
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
    SecureChatCardNoAnimation {
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
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )

            Button(
                onClick = onClearLocalData,
                enabled = !isClearingLocalData,
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.extraSmall,
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.15f),
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
            ) {
                if (isClearingLocalData) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.error
                    )
                } else {
                    Text(
                        text = stringResource(Res.string.base_clear_local_data),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

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
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
        )

        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

@Preview
@Composable
fun DeveloperScreenPreview() {
    SecureChatTheme {
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
                            currentWebSocketUrl = "ws://192.168.178.60:8490/v1/gateway",
                            registryUrl = "http://10.0.2.2:8390",
                            registryAuthorityVerified = true,
                            availableNodes =
                                listOf(
                                    TransportNodeDiagnostic(
                                        nodeId = "1dc6103605070c67",
                                        websocketUrl = "ws://192.168.178.60:8490/v1/gateway",
                                        state = TransportNodeDiagnosticState.CURRENT
                                    ),
                                    TransportNodeDiagnostic(
                                        nodeId = "901d125ea367d974",
                                        websocketUrl = "ws://192.168.178.21:8490/v1/gateway",
                                        state = TransportNodeDiagnosticState.AVAILABLE
                                    )
                                )
                        )
                ),
            onUiEvent = {}
        )
    }
}
