package com.cbgm.sparrow.feature.settings.presentation.network

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.cbgm.sparrow.core.ui.component.SparrowLazyScaffold
import com.cbgm.sparrow.core.ui.theme.SparrowTheme
import com.cbgm.sparrow.core.ui.theme.spacing
import com.cbgm.sparrow.feature.settings.presentation.network.component.AddControlPlaneDialog
import com.cbgm.sparrow.feature.settings.presentation.network.component.ControlPlaneSettingsDivider
import com.cbgm.sparrow.feature.settings.presentation.network.component.ControlPlaneSettingsEntry
import com.cbgm.sparrow.feature.settings.presentation.network.component.ControlPlaneSettingsGroup
import com.cbgm.sparrow.feature.settings.presentation.network.component.ControlPlaneSummaryCard
import com.cbgm.sparrow.feature.settings.presentation.network.component.ControlPlaneTopBar
import com.cbgm.sparrow.feature.settings.presentation.network.component.DirectorySourceCard
import com.cbgm.sparrow.feature.settings.presentation.network.model.ControlPlaneSettingsUiEvent
import com.cbgm.sparrow.feature.settings.presentation.network.model.ControlPlaneSettingsUiState
import com.cbgm.sparrow.feature.settings.presentation.network.model.ControlPlaneUiModel
import com.cbgm.sparrow.feature.settings.presentation.network.model.ControlPlaneUiSource
import com.cbgm.sparrow.feature.settings.presentation.network.model.ControlPlaneUiStatus
import com.cbgm.sparrow.resources.Res
import com.cbgm.sparrow.resources.feature_settings_control_plane_add
import com.cbgm.sparrow.resources.feature_settings_control_planes_discovered
import com.cbgm.sparrow.resources.feature_settings_control_planes_manually_added
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ControlPlaneSettingsScreen(
    uiState: ControlPlaneSettingsUiState,
    onUiEvent: (ControlPlaneSettingsUiEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    SparrowLazyScaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { color ->
            ControlPlaneTopBar(
                containerColor = color,
                isRefreshing = uiState.isRefreshing,
                onBack = { onUiEvent(ControlPlaneSettingsUiEvent.BackClicked) },
                onRefresh = { onUiEvent(ControlPlaneSettingsUiEvent.Refresh) }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onUiEvent(ControlPlaneSettingsUiEvent.AddClicked) },
                shape = CircleShape,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(Res.string.feature_settings_control_plane_add)
                )
            }
        }
    ) { innerPadding, listState ->
        val discovered = uiState.entries.filter { it.source != ControlPlaneUiSource.MANUAL }
        val manuallyAdded = uiState.entries.filter { it.source == ControlPlaneUiSource.MANUAL }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = listState,
            contentPadding = PaddingValues(
                top = innerPadding.calculateTopPadding() + MaterialTheme.spacing.medium,
                bottom = innerPadding.calculateBottomPadding() + MaterialTheme.spacing.times(10)
            ),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium)
        ) {
            item(key = "control-plane-summary") {
                ControlPlaneSummaryCard(
                    availableCount = uiState.availableCount,
                    unavailableCount = uiState.unavailableCount
                )
            }

            item(key = "directory-source") {
                DirectorySourceCard(
                    directoryUrl = uiState.directoryUrl,
                    jsonDirectoryUrl = uiState.jsonDirectoryUrl,
                    directoryError = uiState.directoryError,
                    directoryFailureDetail = uiState.directoryFailureDetail,
                    onEditDirectory = { onUiEvent(ControlPlaneSettingsUiEvent.EditDirectoryClicked) },
                    onRemoveDirectory = { onUiEvent(ControlPlaneSettingsUiEvent.RemoveDirectory) }
                )
            }

            if (discovered.isNotEmpty()) {
                item(key = "discovered-group") {
                    ControlPlaneSettingsGroup(
                        title = stringResource(Res.string.feature_settings_control_planes_discovered)
                    ) {
                        discovered.forEachIndexed { index, entry ->
                            if (index > 0) ControlPlaneSettingsDivider()
                            ControlPlaneSettingsEntry(entry = entry, onRemove = {})
                        }
                    }
                }
            }

            if (manuallyAdded.isNotEmpty()) {
                item(key = "manual-group") {
                    ControlPlaneSettingsGroup(
                        title = stringResource(Res.string.feature_settings_control_planes_manually_added)
                    ) {
                        manuallyAdded.forEachIndexed { index, entry ->
                            if (index > 0) ControlPlaneSettingsDivider()
                            ControlPlaneSettingsEntry(
                                entry = entry,
                                onRemove = { onUiEvent(ControlPlaneSettingsUiEvent.Remove(entry.url)) }
                            )
                        }
                    }
                }
            }
        }
    }

    AddControlPlaneDialog(
        isVisible = uiState.showAddDialog,
        value = uiState.newUrl,
        source = uiState.addSource,
        editingDirectory = uiState.isEditingDirectory,
        onSourceChanged = { onUiEvent(ControlPlaneSettingsUiEvent.AddSourceChanged(it)) },
        error = uiState.addError,
        onValueChanged = { onUiEvent(ControlPlaneSettingsUiEvent.NewUrlChanged(it)) },
        onConfirm = { onUiEvent(ControlPlaneSettingsUiEvent.AddConfirmed) },
        onDismiss = { onUiEvent(ControlPlaneSettingsUiEvent.AddDismissed) }
    )
}

@Preview
@Composable
fun ControlPaneSettingsScreenPreview() {
    SparrowTheme {
        ControlPlaneSettingsScreen(
            uiState = ControlPlaneSettingsUiState(
                entries = listOf(
                    ControlPlaneUiModel(
                        url = "https://example.com",
                        source = ControlPlaneUiSource.MANUAL,
                        canRemove = true,
                        status = ControlPlaneUiStatus.CHECKING
                    ),
                    ControlPlaneUiModel(
                        url = "https://example4.com",
                        source = ControlPlaneUiSource.MANUAL,
                        canRemove = true,
                        status = ControlPlaneUiStatus.AVAILABLE
                    ),
                    ControlPlaneUiModel(
                        url = "https://example3.com",
                        source = ControlPlaneUiSource.MANUAL,
                        canRemove = true,
                        status = ControlPlaneUiStatus.UNREACHABLE
                    ),
                    ControlPlaneUiModel(
                        url = "https://example6.com",
                        source = ControlPlaneUiSource.DIRECTORY,
                        canRemove = true,
                        status = ControlPlaneUiStatus.ACTIVE
                    )
                )
            ),
            onUiEvent = {}
        )
    }
}
