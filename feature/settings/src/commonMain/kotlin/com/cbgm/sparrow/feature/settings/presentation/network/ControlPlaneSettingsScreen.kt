package com.cbgm.sparrow.feature.settings.presentation.network

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.cbgm.sparrow.core.ui.component.SparrowAlertDialog
import com.cbgm.sparrow.core.ui.component.SparrowApprovalButton
import com.cbgm.sparrow.core.ui.component.SparrowInputField
import com.cbgm.sparrow.core.ui.component.SparrowLazyScaffold
import com.cbgm.sparrow.core.ui.component.SparrowOutlinedButton
import com.cbgm.sparrow.core.ui.theme.SparrowTheme
import com.cbgm.sparrow.core.ui.theme.spacing
import com.cbgm.sparrow.feature.settings.presentation.network.components.ControlPlaneListItem
import com.cbgm.sparrow.feature.settings.presentation.network.components.ControlPlaneSummaryCard
import com.cbgm.sparrow.feature.settings.presentation.network.model.ControlPlaneAddSource
import com.cbgm.sparrow.feature.settings.presentation.network.model.ControlPlaneSettingsError
import com.cbgm.sparrow.feature.settings.presentation.network.model.ControlPlaneSettingsUiEvent
import com.cbgm.sparrow.feature.settings.presentation.network.model.ControlPlaneSettingsUiState
import com.cbgm.sparrow.feature.settings.presentation.network.model.ControlPlaneUiModel
import com.cbgm.sparrow.feature.settings.presentation.network.model.ControlPlaneUiSource
import com.cbgm.sparrow.feature.settings.presentation.network.model.ControlPlaneUiStatus
import com.cbgm.sparrow.resources.Res
import com.cbgm.sparrow.resources.base_cancel
import com.cbgm.sparrow.resources.feature_settings_control_plane_add
import com.cbgm.sparrow.resources.feature_settings_control_plane_address
import com.cbgm.sparrow.resources.feature_settings_control_plane_directory_apply
import com.cbgm.sparrow.resources.feature_settings_control_plane_directory_disabled
import com.cbgm.sparrow.resources.feature_settings_control_plane_directory_edit
import com.cbgm.sparrow.resources.feature_settings_control_plane_directory_sync_failed
import com.cbgm.sparrow.resources.feature_settings_control_plane_directory_title
import com.cbgm.sparrow.resources.feature_settings_control_plane_error_duplicate
import com.cbgm.sparrow.resources.feature_settings_control_plane_error_invalid_url
import com.cbgm.sparrow.resources.feature_settings_control_plane_error_keep_one
import com.cbgm.sparrow.resources.feature_settings_control_plane_error_save_failed
import com.cbgm.sparrow.resources.feature_settings_control_plane_json_list
import com.cbgm.sparrow.resources.feature_settings_control_plane_json_unverified
import com.cbgm.sparrow.resources.feature_settings_control_plane_remove
import com.cbgm.sparrow.resources.feature_settings_control_plane_source_manual
import com.cbgm.sparrow.resources.feature_settings_control_planes
import com.cbgm.sparrow.resources.feature_settings_control_planes_discovered
import com.cbgm.sparrow.resources.feature_settings_control_planes_manually_added
import com.cbgm.sparrow.resources.feature_settings_control_planes_refresh
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
        }
    ) { innerPadding, listState ->
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = listState,
                contentPadding =
                    PaddingValues(
                        top = innerPadding.calculateTopPadding(),
                        bottom = innerPadding.calculateBottomPadding() + MaterialTheme.spacing.times(
                            10
                        )
                    )
            ) {
                item(key = "control-plane-summary") {
                    ControlPlaneSummaryCard(uiState = uiState)
                }
                item(key = "directory-source") {
                    val directoryAddress = uiState.directoryUrl.ifBlank { uiState.jsonDirectoryUrl }
                    ListItem(
                        headlineContent = {
                            Text(
                                text = directoryAddress.ifBlank {
                                    stringResource(Res.string.feature_settings_control_plane_directory_disabled)
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                overflow = TextOverflow.Ellipsis,
                                maxLines = 1
                            )
                        },
                        supportingContent = {
                            androidx.compose.foundation.layout.Column {
                                if (uiState.jsonDirectoryUrl.isNotBlank() && uiState.directoryUrl.isBlank()) {
                                    Text(
                                        text = stringResource(Res.string.feature_settings_control_plane_json_unverified),
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                }
                                if (uiState.directoryError != null) {
                                    Text(
                                        text = stringResource(Res.string.feature_settings_control_plane_directory_sync_failed),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                    uiState.directoryFailureDetail?.let { detail ->
                                        Text(
                                            text = detail,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                        },
                        trailingContent = {
                            androidx.compose.foundation.layout.Row {
                                IconButton(onClick = {
                                    onUiEvent(ControlPlaneSettingsUiEvent.EditDirectoryClicked)
                                }) {
                                    Icon(
                                        Icons.Default.Edit,
                                        contentDescription = stringResource(Res.string.feature_settings_control_plane_directory_edit)
                                    )
                                }
                                if (directoryAddress.isNotBlank()) {
                                    IconButton(onClick = {
                                        onUiEvent(ControlPlaneSettingsUiEvent.RemoveDirectory)
                                    }) {
                                        Icon(
                                            Icons.Default.DeleteOutline,
                                            contentDescription = stringResource(Res.string.feature_settings_control_plane_remove)
                                        )
                                    }
                                }
                            }
                        },
                        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.background)
                    )
                }
                val discovered = uiState.entries.filter { it.source != ControlPlaneUiSource.MANUAL }
                if (discovered.isNotEmpty()) {
                    item(key = "discovered-heading") {
                        Text(
                            text = stringResource(Res.string.feature_settings_control_planes_discovered),
                            modifier = Modifier.padding(
                                horizontal = MaterialTheme.spacing.screenPadding,
                                vertical = MaterialTheme.spacing.base
                            ),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                    items(
                        items = discovered,
                        key = { "discovered:${it.url}" }
                    ) { entry ->
                        ControlPlaneListItem(entry = entry, onRemove = {})
                    }
                }
                val manuallyAdded = uiState.entries.filter { it.source == ControlPlaneUiSource.MANUAL }
                if (manuallyAdded.isNotEmpty()) {
                    item(key = "manually-added-heading") {
                        Text(
                            text = stringResource(Res.string.feature_settings_control_planes_manually_added),
                            modifier = Modifier.padding(
                                horizontal = MaterialTheme.spacing.screenPadding,
                                vertical = MaterialTheme.spacing.base
                            ),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                    items(
                        items = manuallyAdded,
                        key = { "manual:${it.url}" }
                    ) { entry ->
                        ControlPlaneListItem(
                            entry = entry,
                            onRemove = { onUiEvent(ControlPlaneSettingsUiEvent.Remove(entry.url)) }
                        )
                    }
                }
            }

            FloatingActionButton(
                onClick = { onUiEvent(ControlPlaneSettingsUiEvent.AddClicked) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier =
                    Modifier
                        .align(Alignment.BottomEnd)
                        .padding(MaterialTheme.spacing.screenPadding)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null
                )
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ControlPlaneTopBar(
    containerColor: Color,
    isRefreshing: Boolean,
    onBack: () -> Unit,
    onRefresh: () -> Unit
) {
    CenterAlignedTopAppBar(
        title = {
            Text(
                text = stringResource(Res.string.feature_settings_control_planes),
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
                containerColor = containerColor,
                scrolledContainerColor = containerColor,
                titleContentColor = MaterialTheme.colorScheme.onBackground,
                navigationIconContentColor = MaterialTheme.colorScheme.onBackground
            )
    )
}

@Composable
private fun AddControlPlaneDialog(
    isVisible: Boolean,
    value: String,
    source: ControlPlaneAddSource,
    editingDirectory: Boolean,
    onSourceChanged: (ControlPlaneAddSource) -> Unit,
    error: ControlPlaneSettingsError?,
    onValueChanged: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    SparrowAlertDialog(
        isVisible = isVisible,
        onDismissRequest = onDismiss,
        title = stringResource(
            if (editingDirectory) {
                Res.string.feature_settings_control_plane_directory_edit
            } else {
                Res.string.feature_settings_control_plane_add
            }
        ),
        text = {
            androidx.compose.foundation.layout.Column {
                androidx.compose.foundation.layout.Column {
                    if (!editingDirectory) {
                        FilterChip(
                            selected = source == ControlPlaneAddSource.PLANE,
                            onClick = { onSourceChanged(ControlPlaneAddSource.PLANE) },
                            label = { Text(stringResource(Res.string.feature_settings_control_plane_source_manual)) }
                        )
                    }
                    FilterChip(
                        selected = source == ControlPlaneAddSource.SIGNED_DIRECTORY,
                        onClick = { onSourceChanged(ControlPlaneAddSource.SIGNED_DIRECTORY) },
                        label = { Text(stringResource(Res.string.feature_settings_control_plane_directory_title)) }
                    )
                    FilterChip(
                        selected = source == ControlPlaneAddSource.JSON_LIST,
                        onClick = { onSourceChanged(ControlPlaneAddSource.JSON_LIST) },
                        label = { Text(stringResource(Res.string.feature_settings_control_plane_json_list)) }
                    )
                }
                if (source == ControlPlaneAddSource.JSON_LIST) {
                    Text(
                        stringResource(Res.string.feature_settings_control_plane_json_unverified),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                SparrowInputField(
                    value = value,
                    onValueChange = onValueChanged,
                    modifier =
                        Modifier.fillMaxWidth().padding(
                            top = MaterialTheme.spacing.small,
                            bottom = MaterialTheme.spacing.base
                        ),
                    label = stringResource(Res.string.feature_settings_control_plane_address),
                    isError = error != null,
                    isSingleLine = true,
                    errorText = error?.let { addErrorText(it) } ?: ""
                )
            }
        },
        confirmButton = {
            SparrowApprovalButton(
                onClick = onConfirm,
                fillMaxWidth = false,
                text = stringResource(
                    if (editingDirectory) {
                        Res.string.feature_settings_control_plane_directory_apply
                    } else {
                        Res.string.feature_settings_control_plane_add
                    }
                )
            )
        },
        dismissButton = {
            SparrowOutlinedButton(
                onClick = onDismiss,
                fillMaxWidth = false,
                text = stringResource(Res.string.base_cancel)
            )
        }
    )
}

@Composable
private fun addErrorText(error: ControlPlaneSettingsError): String =
    when (error) {
        ControlPlaneSettingsError.INVALID_URL ->
            stringResource(Res.string.feature_settings_control_plane_error_invalid_url)

        ControlPlaneSettingsError.DUPLICATE ->
            stringResource(Res.string.feature_settings_control_plane_error_duplicate)

        ControlPlaneSettingsError.KEEP_ONE ->
            stringResource(Res.string.feature_settings_control_plane_error_keep_one)

        ControlPlaneSettingsError.SAVE_FAILED ->
            stringResource(Res.string.feature_settings_control_plane_error_save_failed)
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
