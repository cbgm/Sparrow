package com.cbgm.sparrow.feature.settings.presentation.network.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.cbgm.sparrow.core.ui.theme.Alpha
import com.cbgm.sparrow.core.ui.theme.Dimens
import com.cbgm.sparrow.core.ui.theme.SparrowTheme
import com.cbgm.sparrow.core.ui.theme.circle
import com.cbgm.sparrow.core.ui.theme.spacing
import com.cbgm.sparrow.feature.settings.presentation.network.model.ControlPlaneSettingsUiState
import com.cbgm.sparrow.feature.settings.presentation.network.model.ControlPlaneUiModel
import com.cbgm.sparrow.feature.settings.presentation.network.model.ControlPlaneUiSource
import com.cbgm.sparrow.feature.settings.presentation.network.model.ControlPlaneUiStatus
import com.cbgm.sparrow.resources.Res
import com.cbgm.sparrow.resources.feature_settings_control_plane_remove
import com.cbgm.sparrow.resources.feature_settings_control_plane_source_directory
import com.cbgm.sparrow.resources.feature_settings_control_plane_source_manual
import com.cbgm.sparrow.resources.feature_settings_control_plane_source_manual_and_directory
import com.cbgm.sparrow.resources.feature_settings_control_plane_status_active
import com.cbgm.sparrow.resources.feature_settings_control_plane_status_available
import com.cbgm.sparrow.resources.feature_settings_control_plane_status_checking
import com.cbgm.sparrow.resources.feature_settings_control_plane_status_unreachable
import com.cbgm.sparrow.resources.feature_settings_control_planes_auto_health
import com.cbgm.sparrow.resources.feature_settings_control_planes_summary
import org.jetbrains.compose.resources.stringResource

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
            color = MaterialTheme.colorScheme.onSurfaceVariant
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
                    .padding(start = MaterialTheme.spacing.listDividerStart),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = Alpha.itemDivider)
        )
    }
}

@Composable
private fun StatusDot(status: ControlPlaneUiStatus) {
    Box(
        modifier =
            Modifier
                .size(Dimens.ControlPlaneSettingsScreen.statusIndicatorSize)
                .background(statusColor(status), MaterialTheme.shapes.circle)
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
        ControlPlaneUiStatus.ACTIVE -> MaterialTheme.colorScheme.primary
        ControlPlaneUiStatus.AVAILABLE -> MaterialTheme.colorScheme.tertiary
        ControlPlaneUiStatus.UNREACHABLE -> MaterialTheme.colorScheme.error
        ControlPlaneUiStatus.CHECKING -> MaterialTheme.colorScheme.onSurfaceVariant
    }

@Preview
@Composable
fun ControlPaneCardSummaryPreview() {
    SparrowTheme {
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
    SparrowTheme {
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
