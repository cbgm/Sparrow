package com.cbgm.sparrow.feature.settings.presentation.network.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.cbgm.sparrow.core.ui.component.SparrowAlertDialog
import com.cbgm.sparrow.core.ui.component.SparrowDialogAction
import com.cbgm.sparrow.core.ui.component.SparrowInputField
import com.cbgm.sparrow.core.ui.component.SparrowTabs
import com.cbgm.sparrow.core.ui.theme.Dimens
import com.cbgm.sparrow.core.ui.theme.SparrowTheme
import com.cbgm.sparrow.core.ui.theme.spacing
import com.cbgm.sparrow.feature.settings.presentation.network.model.ControlPlaneAddSource
import com.cbgm.sparrow.feature.settings.presentation.network.model.ControlPlaneSettingsError
import com.cbgm.sparrow.resources.Res
import com.cbgm.sparrow.resources.base_cancel
import com.cbgm.sparrow.resources.feature_settings_control_plane_add
import com.cbgm.sparrow.resources.feature_settings_control_plane_add_placeholder
import com.cbgm.sparrow.resources.feature_settings_control_plane_add_support_directory
import com.cbgm.sparrow.resources.feature_settings_control_plane_add_support_list
import com.cbgm.sparrow.resources.feature_settings_control_plane_add_support_manual
import com.cbgm.sparrow.resources.feature_settings_control_plane_add_tab_directory
import com.cbgm.sparrow.resources.feature_settings_control_plane_add_tab_list
import com.cbgm.sparrow.resources.feature_settings_control_plane_add_tab_manual
import com.cbgm.sparrow.resources.feature_settings_control_plane_address
import com.cbgm.sparrow.resources.feature_settings_control_plane_directory_apply
import com.cbgm.sparrow.resources.feature_settings_control_plane_directory_edit
import com.cbgm.sparrow.resources.feature_settings_control_plane_error_duplicate
import com.cbgm.sparrow.resources.feature_settings_control_plane_error_invalid_url
import com.cbgm.sparrow.resources.feature_settings_control_plane_error_keep_one
import com.cbgm.sparrow.resources.feature_settings_control_plane_error_save_failed
import com.cbgm.sparrow.resources.feature_settings_control_plane_json_unverified
import org.jetbrains.compose.resources.stringResource

@Composable
fun AddControlPlaneDialog(
    isVisible: Boolean,
    value: String,
    source: ControlPlaneAddSource,
    editingDirectory: Boolean,
    onSourceChanged: (ControlPlaneAddSource) -> Unit,
    error: ControlPlaneSettingsError?,
    onValueChanged: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    failureDetail: String? = null,
    isLoading: Boolean = false
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
            Column {
                AddControlPlaneTabs(
                    selectedSource = source,
                    editingDirectory = editingDirectory,
                    onSourceChanged = onSourceChanged
                )
                if (source == ControlPlaneAddSource.JSON_LIST) {
                    Text(
                        stringResource(Res.string.feature_settings_control_plane_json_unverified),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = MaterialTheme.spacing.small)
                    )
                }
                SparrowInputField(
                    value = value,
                    onValueChange = onValueChanged,
                    modifier = Modifier.fillMaxWidth().padding(
                        top = MaterialTheme.spacing.small,
                        bottom = MaterialTheme.spacing.base
                    ),
                    placeholderText = stringResource(Res.string.feature_settings_control_plane_add_placeholder),
                    supportingText = when (source) {
                        ControlPlaneAddSource.PLANE -> stringResource(Res.string.feature_settings_control_plane_add_support_manual)
                        ControlPlaneAddSource.SIGNED_DIRECTORY -> stringResource(Res.string.feature_settings_control_plane_add_support_directory)
                        ControlPlaneAddSource.JSON_LIST -> stringResource(Res.string.feature_settings_control_plane_add_support_list)
                    },
                    label = stringResource(Res.string.feature_settings_control_plane_address),
                    isError = error != null,
                    isSingleLine = true,
                    errorText = error?.let { addErrorText(it) } ?: ""
                )
                if (error == ControlPlaneSettingsError.SAVE_FAILED && !failureDetail.isNullOrBlank()) {
                    Text(
                        text = failureDetail,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            SparrowDialogAction(
                onClick = onConfirm,
                enabled = !isLoading,
                text = if (isLoading) {
                    ""
                } else {
                    stringResource(
                        if (editingDirectory) {
                            Res.string.feature_settings_control_plane_directory_apply
                        } else {
                            Res.string.feature_settings_control_plane_add
                        }
                    )
                },
                content = {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(Dimens.Base.progressButtonIndicatorSize),
                            strokeWidth = Dimens.Base.progressIndicatorStrokeWidth,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            )
        },
        dismissButton = {
            SparrowDialogAction(
                onClick = onDismiss,
                text = stringResource(Res.string.base_cancel),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    )
}

@Composable
private fun AddControlPlaneTabs(
    selectedSource: ControlPlaneAddSource,
    editingDirectory: Boolean,
    onSourceChanged: (ControlPlaneAddSource) -> Unit
) {
    val sources = if (editingDirectory) {
        listOf(ControlPlaneAddSource.SIGNED_DIRECTORY, ControlPlaneAddSource.JSON_LIST)
    } else {
        listOf(
            ControlPlaneAddSource.PLANE,
            ControlPlaneAddSource.SIGNED_DIRECTORY,
            ControlPlaneAddSource.JSON_LIST
        )
    }
    val labels = sources.map { source ->
        when (source) {
            ControlPlaneAddSource.PLANE -> stringResource(Res.string.feature_settings_control_plane_add_tab_manual)
            ControlPlaneAddSource.SIGNED_DIRECTORY -> stringResource(Res.string.feature_settings_control_plane_add_tab_directory)
            ControlPlaneAddSource.JSON_LIST -> stringResource(Res.string.feature_settings_control_plane_add_tab_list)
        }
    }
    val selectedIndex = sources.indexOf(selectedSource).coerceAtLeast(0)

    SparrowTabs(
        labels = labels,
        selectedIndex = selectedIndex,
        onSelect = { index -> onSourceChanged(sources[index]) },
        isFullUnderLine = true
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
private fun AddControlPlaneDialogPreview() {
    SparrowTheme {
        AddControlPlaneDialog(
            isVisible = true,
            value = "https://example.com",
            source = ControlPlaneAddSource.PLANE,
            editingDirectory = false,
            onSourceChanged = {},
            error = null,
            onValueChanged = {},
            onConfirm = {},
            onDismiss = {}
        )
    }
}
