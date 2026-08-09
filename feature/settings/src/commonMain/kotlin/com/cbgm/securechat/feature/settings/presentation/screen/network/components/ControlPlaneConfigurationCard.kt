package com.cbgm.securechat.feature.settings.presentation.screen.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.cbgm.securechat.core.ui.component.SecureChatCardNoAnimation
import com.cbgm.securechat.core.ui.component.SecureChatOutlinedButton
import com.cbgm.securechat.core.ui.theme.SecureChatTheme
import com.cbgm.securechat.core.ui.theme.spacing
import com.cbgm.securechat.feature.settings.presentation.model.ControlPlaneDirectoryError
import com.cbgm.securechat.feature.settings.presentation.model.ControlPlaneSettingsError
import com.cbgm.securechat.feature.settings.presentation.model.ControlPlaneSettingsUiState
import com.cbgm.securechat.feature.settings.presentation.model.ControlPlaneUiModel
import com.cbgm.securechat.feature.settings.presentation.model.ControlPlaneUiSource
import com.cbgm.securechat.feature.settings.presentation.model.ControlPlaneUiStatus
import com.cbgm.securechat.resources.Res
import com.cbgm.securechat.resources.base_cancel
import com.cbgm.securechat.resources.feature_settings_control_plane_add
import com.cbgm.securechat.resources.feature_settings_control_plane_address
import com.cbgm.securechat.resources.feature_settings_control_plane_directory_apply
import com.cbgm.securechat.resources.feature_settings_control_plane_directory_description
import com.cbgm.securechat.resources.feature_settings_control_plane_directory_disabled
import com.cbgm.securechat.resources.feature_settings_control_plane_directory_error_invalid_url
import com.cbgm.securechat.resources.feature_settings_control_plane_directory_error_save_failed
import com.cbgm.securechat.resources.feature_settings_control_plane_directory_sync_failed
import com.cbgm.securechat.resources.feature_settings_control_plane_directory_synced_count
import com.cbgm.securechat.resources.feature_settings_control_plane_directory_title
import com.cbgm.securechat.resources.feature_settings_control_plane_directory_url
import com.cbgm.securechat.resources.feature_settings_control_plane_error_duplicate
import com.cbgm.securechat.resources.feature_settings_control_plane_error_invalid_url
import com.cbgm.securechat.resources.feature_settings_control_plane_error_keep_one
import com.cbgm.securechat.resources.feature_settings_control_plane_error_save_failed
import com.cbgm.securechat.resources.feature_settings_control_plane_remove
import com.cbgm.securechat.resources.feature_settings_control_plane_source_directory
import com.cbgm.securechat.resources.feature_settings_control_plane_source_manual
import com.cbgm.securechat.resources.feature_settings_control_plane_source_manual_and_directory
import com.cbgm.securechat.resources.feature_settings_control_plane_status_active
import com.cbgm.securechat.resources.feature_settings_control_plane_status_available
import com.cbgm.securechat.resources.feature_settings_control_plane_status_checking
import com.cbgm.securechat.resources.feature_settings_control_plane_status_unreachable
import com.cbgm.securechat.resources.feature_settings_control_planes_auto_health
import com.cbgm.securechat.resources.feature_settings_control_planes_summary
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun ControlPlaneDirectoryCard(
    uiState: ControlPlaneSettingsUiState,
    onDirectoryUrlChanged: (String) -> Unit,
    onApply: () -> Unit
) {
    SecureChatCardNoAnimation(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = MaterialTheme.spacing.screenPadding)
                .padding(top = MaterialTheme.spacing.small)
    ) {
        Column(
            modifier = Modifier.padding(MaterialTheme.spacing.medium)
        ) {
            Text(
                text = stringResource(Res.string.feature_settings_control_plane_directory_title),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = stringResource(Res.string.feature_settings_control_plane_directory_description),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = MaterialTheme.spacing.base)
            )

            val StartupPhoneFieldBackground = Color(0xFF0B2035)

            OutlinedTextField(
                value = uiState.directoryDraft,
                onValueChange = onDirectoryUrlChanged,
                modifier =
                    Modifier.fillMaxWidth().padding(
                        top = MaterialTheme.spacing.small,
                        bottom = MaterialTheme.spacing.base
                    ),
                label = {
                    Text(text = stringResource(Res.string.feature_settings_control_plane_directory_url))
                },
                isError = uiState.directoryError != null,
                singleLine = true,
                textStyle =
                    MaterialTheme.typography.bodyMedium.copy(
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    ),
                colors =
                    OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        disabledTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.60f),
                        focusedContainerColor = StartupPhoneFieldBackground,
                        unfocusedContainerColor = StartupPhoneFieldBackground,
                        errorContainerColor = StartupPhoneFieldBackground,
                        focusedBorderColor = MaterialTheme.colorScheme.secondary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.18f),
                        errorBorderColor = MaterialTheme.colorScheme.error,
                        focusedLabelColor = MaterialTheme.colorScheme.secondary,
                        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.76f),
                        errorLabelColor = MaterialTheme.colorScheme.error,
                        focusedPlaceholderColor =
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                alpha = 0.38f
                            ),
                        unfocusedPlaceholderColor =
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                alpha = 0.38f
                            ),
                        focusedSupportingTextColor =
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                alpha = 0.66f
                            ),
                        unfocusedSupportingTextColor =
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                alpha = 0.66f
                            ),
                        errorSupportingTextColor = MaterialTheme.colorScheme.error,
                        cursorColor = MaterialTheme.colorScheme.secondary,
                        errorCursorColor = MaterialTheme.colorScheme.error
                    )
            )

            DirectoryStatus(uiState)
            SecureChatOutlinedButton(
                onClick = onApply,
                modifier = Modifier.fillMaxWidth().padding(top = MaterialTheme.spacing.small),
                enabled = !uiState.isDirectorySyncing,
                content = {
                    if (uiState.isDirectorySyncing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.size(MaterialTheme.spacing.base))
                    }
                    Text(stringResource(Res.string.feature_settings_control_plane_directory_apply))
                }
            )
        }
    }
}

@Composable
internal fun ControlPlaneSummaryCard(uiState: ControlPlaneSettingsUiState) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(MaterialTheme.spacing.screenPadding),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.base)
    ) {
        Text(
            text =
                stringResource(
                    Res.string.feature_settings_control_planes_summary,
                    uiState.availableCount,
                    uiState.unavailableCount
                ),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = stringResource(Res.string.feature_settings_control_planes_auto_health),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

@Composable
internal fun ControlPlaneListItem(
    entry: ControlPlaneUiModel,
    onRemove: () -> Unit
) {
    Column {
        ListItem(
            modifier = Modifier.fillMaxWidth(),
            leadingContent = { StatusDot(entry.status) },
            headlineContent = {
                Text(
                    text = entry.url,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            supportingContent = {
                Text(
                    text = "${statusText(entry.status)} · ${sourceText(entry.source)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = statusColor(entry.status)
                )
            },
            trailingContent = {
                if (entry.canRemove) {
                    IconButton(onClick = onRemove) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = stringResource(Res.string.feature_settings_control_plane_remove)
                        )
                    }
                }
            },
            colors =
                ListItemDefaults.colors(
                    containerColor = MaterialTheme.colorScheme.background,
                    contentColor = MaterialTheme.colorScheme.onBackground
                )
        )
        HorizontalDivider(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(start = 80.dp),
            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.05f)
        )
    }
}

@Composable
internal fun AddControlPlaneDialog(
    value: String,
    error: ControlPlaneSettingsError?,
    onValueChanged: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.feature_settings_control_plane_add)) },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChanged,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(Res.string.feature_settings_control_plane_address)) },
                singleLine = true,
                isError = error != null,
                supportingText = error?.let { { Text(addErrorText(it)) } }
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(Res.string.feature_settings_control_plane_add))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.base_cancel))
            }
        }
    )
}

@Composable
private fun DirectoryStatus(uiState: ControlPlaneSettingsUiState) {
    val text =
        when {
            uiState.directoryError == ControlPlaneDirectoryError.SYNC_FAILED ->
                stringResource(Res.string.feature_settings_control_plane_directory_sync_failed)

            uiState.directoryError == ControlPlaneDirectoryError.INVALID_URL ->
                stringResource(Res.string.feature_settings_control_plane_directory_error_invalid_url)

            uiState.directoryError == ControlPlaneDirectoryError.SAVE_FAILED ->
                stringResource(Res.string.feature_settings_control_plane_directory_error_save_failed)

            uiState.directoryUrl.isBlank() ->
                stringResource(Res.string.feature_settings_control_plane_directory_disabled)

            uiState.lastDirectoryCount != null ->
                stringResource(
                    Res.string.feature_settings_control_plane_directory_synced_count,
                    uiState.lastDirectoryCount
                )

            else -> uiState.directoryUrl
        }
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color =
            if (uiState.directoryError != null) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
    )
}

@Composable
private fun StatusDot(status: ControlPlaneUiStatus) {
    Box(
        modifier =
            Modifier
                .size(12.dp)
                .background(statusColor(status), CircleShape)
    )
}

@Composable
private fun statusText(status: ControlPlaneUiStatus): String =
    when (status) {
        ControlPlaneUiStatus.ACTIVE -> stringResource(Res.string.feature_settings_control_plane_status_active)
        ControlPlaneUiStatus.AVAILABLE ->
            stringResource(Res.string.feature_settings_control_plane_status_available)

        ControlPlaneUiStatus.UNREACHABLE ->
            stringResource(Res.string.feature_settings_control_plane_status_unreachable)

        ControlPlaneUiStatus.CHECKING ->
            stringResource(Res.string.feature_settings_control_plane_status_checking)
    }

@Composable
private fun sourceText(source: ControlPlaneUiSource): String =
    when (source) {
        ControlPlaneUiSource.MANUAL -> stringResource(Res.string.feature_settings_control_plane_source_manual)
        ControlPlaneUiSource.DIRECTORY ->
            stringResource(Res.string.feature_settings_control_plane_source_directory)

        ControlPlaneUiSource.MANUAL_AND_DIRECTORY ->
            stringResource(Res.string.feature_settings_control_plane_source_manual_and_directory)
    }

@Composable
private fun statusColor(status: ControlPlaneUiStatus): Color =
    when (status) {
        ControlPlaneUiStatus.ACTIVE -> MaterialTheme.colorScheme.secondary
        ControlPlaneUiStatus.AVAILABLE -> MaterialTheme.colorScheme.tertiary
        ControlPlaneUiStatus.UNREACHABLE -> MaterialTheme.colorScheme.error
        ControlPlaneUiStatus.CHECKING -> MaterialTheme.colorScheme.onSurfaceVariant
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
fun ControlPaneCardPreview() {
    SecureChatTheme {
        ControlPlaneDirectoryCard(
            uiState = ControlPlaneSettingsUiState(),
            onDirectoryUrlChanged = {},
            onApply = {}
        )
    }
}

@Preview
@Composable
fun ControlPaneCardSummaryPreview() {
    SecureChatTheme {
        ControlPlaneSummaryCard(
            uiState =
                ControlPlaneSettingsUiState(
                    entries =
                        listOf(
                            ControlPlaneUiModel(
                                url = "https://example.com",
                                status = ControlPlaneUiStatus.ACTIVE,
                                source = ControlPlaneUiSource.DIRECTORY,
                                canRemove = false
                            ),
                            ControlPlaneUiModel(
                                url = "https://example.com",
                                status = ControlPlaneUiStatus.UNREACHABLE,
                                source = ControlPlaneUiSource.DIRECTORY,
                                canRemove = true
                            ),
                            ControlPlaneUiModel(
                                url = "https://example.com",
                                status = ControlPlaneUiStatus.CHECKING,
                                source = ControlPlaneUiSource.DIRECTORY,
                                canRemove = true
                            )
                        )
                )
        )
    }
}

@Preview
@Composable
fun ControlPaneListItemPreview() {
    SecureChatTheme {
        Column {
            ControlPlaneListItem(
                entry =
                    ControlPlaneUiModel(
                        url = "https://example.com",
                        status = ControlPlaneUiStatus.ACTIVE,
                        source = ControlPlaneUiSource.DIRECTORY,
                        canRemove = false
                    ),
                onRemove = {}
            )
            ControlPlaneListItem(
                entry =
                    ControlPlaneUiModel(
                        url = "https://example.com",
                        status = ControlPlaneUiStatus.AVAILABLE,
                        source = ControlPlaneUiSource.DIRECTORY,
                        canRemove = true
                    ),
                onRemove = {}
            )
            ControlPlaneListItem(
                entry =
                    ControlPlaneUiModel(
                        url = "https://example.com",
                        status = ControlPlaneUiStatus.UNREACHABLE,
                        source = ControlPlaneUiSource.DIRECTORY,
                        canRemove = true
                    ),
                onRemove = {}
            )
            ControlPlaneListItem(
                entry =
                    ControlPlaneUiModel(
                        url = "https://example.com",
                        status = ControlPlaneUiStatus.CHECKING,
                        source = ControlPlaneUiSource.DIRECTORY,
                        canRemove = true
                    ),
                onRemove = {}
            )
        }
    }
}
