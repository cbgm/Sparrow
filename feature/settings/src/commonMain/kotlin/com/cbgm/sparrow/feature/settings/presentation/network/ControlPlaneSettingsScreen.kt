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
import androidx.compose.ui.graphics.Color
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
import com.cbgm.sparrow.feature.settings.presentation.network.model.ControlPlaneSettingsError
import com.cbgm.sparrow.feature.settings.presentation.network.model.ControlPlaneSettingsUiEvent
import com.cbgm.sparrow.feature.settings.presentation.network.model.ControlPlaneSettingsUiState
import com.cbgm.sparrow.resources.Res
import com.cbgm.sparrow.resources.base_cancel
import com.cbgm.sparrow.resources.feature_settings_control_plane_add
import com.cbgm.sparrow.resources.feature_settings_control_plane_address
import com.cbgm.sparrow.resources.feature_settings_control_plane_error_duplicate
import com.cbgm.sparrow.resources.feature_settings_control_plane_error_invalid_url
import com.cbgm.sparrow.resources.feature_settings_control_plane_error_keep_one
import com.cbgm.sparrow.resources.feature_settings_control_plane_error_save_failed
import com.cbgm.sparrow.resources.feature_settings_control_planes
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
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
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
    value: String,
    error: ControlPlaneSettingsError?,
    onValueChanged: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    SparrowAlertDialog(
        onDismissRequest = onDismiss,
        title = stringResource(Res.string.feature_settings_control_plane_add),
        text = {
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
        },
        confirmButton = {
            SparrowApprovalButton(
                onClick = onConfirm,
                fillMaxWidth = false,
                text = stringResource(Res.string.feature_settings_control_plane_add)
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
            uiState = ControlPlaneSettingsUiState(),
            onUiEvent = {}
        )
    }
}
