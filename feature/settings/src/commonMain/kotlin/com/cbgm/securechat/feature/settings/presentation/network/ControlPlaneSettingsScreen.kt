package com.cbgm.securechat.feature.settings.presentation.network

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.cbgm.securechat.core.ui.component.SecureChatLazyScaffold
import com.cbgm.securechat.core.ui.theme.SecureChatTheme
import com.cbgm.securechat.core.ui.theme.spacing
import com.cbgm.securechat.feature.settings.presentation.network.components.AddControlPlaneDialog
import com.cbgm.securechat.feature.settings.presentation.network.components.ControlPlaneDirectoryCard
import com.cbgm.securechat.feature.settings.presentation.network.components.ControlPlaneListItem
import com.cbgm.securechat.feature.settings.presentation.network.components.ControlPlaneSummaryCard
import com.cbgm.securechat.feature.settings.presentation.network.model.ControlPlaneSettingsUiEvent
import com.cbgm.securechat.feature.settings.presentation.network.model.ControlPlaneSettingsUiState
import com.cbgm.securechat.resources.Res
import com.cbgm.securechat.resources.feature_settings_control_plane_add
import com.cbgm.securechat.resources.feature_settings_control_planes
import com.cbgm.securechat.resources.feature_settings_control_planes_refresh
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ControlPlaneSettingsScreen(
    uiState: ControlPlaneSettingsUiState,
    onUiEvent: (ControlPlaneSettingsUiEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    SecureChatLazyScaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            ControlPlaneTopBar(
                isRefreshing = uiState.isRefreshing,
                onBack = { onUiEvent(ControlPlaneSettingsUiEvent.BackClicked) },
                onRefresh = { onUiEvent(ControlPlaneSettingsUiEvent.Refresh) }
            )
        }
    ) { innerPadding, listState ->
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = listState,
                contentPadding =
                    PaddingValues(
                        top = innerPadding.calculateTopPadding(),
                        bottom = innerPadding.calculateBottomPadding() + MaterialTheme.spacing.times(10)
                    )
            ) {
                item {
                    ControlPlaneDirectoryCard(
                        uiState = uiState,
                        onDirectoryUrlChanged = {
                            onUiEvent(ControlPlaneSettingsUiEvent.DirectoryUrlChanged(it))
                        },
                        onApply = { onUiEvent(ControlPlaneSettingsUiEvent.DirectoryApply) }
                    )
                }

                item {
                    ControlPlaneSummaryCard(uiState = uiState)
                }

                items(
                    items = uiState.entries,
                    key = { entry -> entry.url }
                ) { entry ->
                    ControlPlaneListItem(
                        entry = entry,
                        onRemove = { onUiEvent(ControlPlaneSettingsUiEvent.Remove(entry.url)) }
                    )
                }
            }

            FloatingActionButton(
                onClick = { onUiEvent(ControlPlaneSettingsUiEvent.AddClicked) },
                containerColor = MaterialTheme.colorScheme.secondary,
                contentColor = MaterialTheme.colorScheme.background,
                modifier =
                    Modifier
                        .align(Alignment.BottomEnd)
                        .padding(MaterialTheme.spacing.screenPadding)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(Res.string.feature_settings_control_plane_add)
                )
            }
        }
    }

    if (uiState.showAddDialog) {
        AddControlPlaneDialog(
            value = uiState.newUrl,
            error = uiState.addError,
            onValueChanged = { onUiEvent(ControlPlaneSettingsUiEvent.NewUrlChanged(it)) },
            onConfirm = { onUiEvent(ControlPlaneSettingsUiEvent.AddConfirmed) },
            onDismiss = { onUiEvent(ControlPlaneSettingsUiEvent.AddDismissed) }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ControlPlaneTopBar(
    isRefreshing: Boolean,
    onBack: () -> Unit,
    onRefresh: () -> Unit
) {
    CenterAlignedTopAppBar(
        title = {
            Text(
                text = stringResource(Res.string.feature_settings_control_planes),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold
            )
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = null
                )
            }
        },
        actions = {
            IconButton(onClick = onRefresh, enabled = !isRefreshing) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = stringResource(Res.string.feature_settings_control_planes_refresh)
                )
            }
        },
        colors =
            TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background,
                titleContentColor = MaterialTheme.colorScheme.onBackground,
                navigationIconContentColor = MaterialTheme.colorScheme.onBackground
            )
    )
}

@Preview
@Composable
fun ControlPaneSettingsScreenPreview() {
    SecureChatTheme {
        ControlPlaneSettingsScreen(
            uiState = ControlPlaneSettingsUiState(),
            onUiEvent = {}
        )
    }
}
