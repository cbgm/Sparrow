package com.cbgm.sparrow.feature.settings.presentation.network.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.cbgm.sparrow.core.ui.component.SparrowAlertDialog
import com.cbgm.sparrow.core.ui.component.SparrowApprovalButton
import com.cbgm.sparrow.core.ui.component.SparrowInputField
import com.cbgm.sparrow.core.ui.component.SparrowOutlinedButton
import com.cbgm.sparrow.core.ui.theme.SparrowTheme
import com.cbgm.sparrow.core.ui.theme.spacing
import com.cbgm.sparrow.feature.settings.presentation.network.model.ControlPlaneAddSource
import com.cbgm.sparrow.feature.settings.presentation.network.model.ControlPlaneSettingsError
import com.cbgm.sparrow.resources.Res
import com.cbgm.sparrow.resources.base_cancel
import com.cbgm.sparrow.resources.feature_settings_control_plane_add
import com.cbgm.sparrow.resources.feature_settings_control_plane_address
import com.cbgm.sparrow.resources.feature_settings_control_plane_directory_apply
import com.cbgm.sparrow.resources.feature_settings_control_plane_directory_edit
import com.cbgm.sparrow.resources.feature_settings_control_plane_directory_title
import com.cbgm.sparrow.resources.feature_settings_control_plane_error_duplicate
import com.cbgm.sparrow.resources.feature_settings_control_plane_error_invalid_url
import com.cbgm.sparrow.resources.feature_settings_control_plane_error_keep_one
import com.cbgm.sparrow.resources.feature_settings_control_plane_error_save_failed
import com.cbgm.sparrow.resources.feature_settings_control_plane_json_list
import com.cbgm.sparrow.resources.feature_settings_control_plane_json_unverified
import com.cbgm.sparrow.resources.feature_settings_control_plane_source_manual
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
            Column {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (!editingDirectory) {
                        AdaptedFilterChip(
                            selectedSource = source,
                            targetSource = ControlPlaneAddSource.PLANE,
                            label = stringResource(Res.string.feature_settings_control_plane_source_manual),
                            onSourceChanged = onSourceChanged
                        )
                    }

                    AdaptedFilterChip(
                        selectedSource = source,
                        targetSource = ControlPlaneAddSource.SIGNED_DIRECTORY,
                        label = stringResource(Res.string.feature_settings_control_plane_directory_title),
                        onSourceChanged = onSourceChanged
                    )

                    AdaptedFilterChip(
                        selectedSource = source,
                        targetSource = ControlPlaneAddSource.JSON_LIST,
                        label = stringResource(Res.string.feature_settings_control_plane_json_list),
                        onSourceChanged = onSourceChanged
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
                    modifier = Modifier.fillMaxWidth().padding(
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
            Row(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.base)) {
                SparrowApprovalButton(
                    onClick = onConfirm,
                    modifier = Modifier.weight(1f),
                    text = stringResource(
                        if (editingDirectory) {
                            Res.string.feature_settings_control_plane_directory_apply
                        } else {
                            Res.string.feature_settings_control_plane_add
                        }
                    )
                )
                SparrowOutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    text = stringResource(Res.string.base_cancel)
                )
            }
        },
        dismissButton = {}
    )
}

@Composable
private fun AdaptedFilterChip(
    selectedSource: ControlPlaneAddSource,
    targetSource: ControlPlaneAddSource,
    label: String,
    onSourceChanged: (ControlPlaneAddSource) -> Unit,
    modifier: Modifier = Modifier
) {
    FilterChip(
        selected = selectedSource == targetSource,
        onClick = { onSourceChanged(targetSource) },
        modifier = modifier.fillMaxWidth(),
        label = {
            Text(
                text = label,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
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
