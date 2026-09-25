package com.cbgm.sparrow.feature.settings.presentation.network.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.cbgm.sparrow.core.ui.component.SparrowCardNoAnimation
import com.cbgm.sparrow.core.ui.theme.Alpha
import com.cbgm.sparrow.core.ui.theme.Dimens
import com.cbgm.sparrow.core.ui.theme.SparrowTheme
import com.cbgm.sparrow.core.ui.theme.spacing
import com.cbgm.sparrow.feature.settings.presentation.network.model.ControlPlaneUiModel
import com.cbgm.sparrow.feature.settings.presentation.network.model.ControlPlaneUiSource
import com.cbgm.sparrow.feature.settings.presentation.network.model.ControlPlaneUiStatus
import com.cbgm.sparrow.resources.Res
import com.cbgm.sparrow.resources.feature_settings_control_plane_remove
import com.cbgm.sparrow.resources.feature_settings_control_plane_source_manual
import com.cbgm.sparrow.resources.feature_settings_control_plane_status_active
import com.cbgm.sparrow.resources.feature_settings_control_plane_status_available
import com.cbgm.sparrow.resources.feature_settings_control_plane_status_checking
import com.cbgm.sparrow.resources.feature_settings_control_plane_status_unreachable
import com.cbgm.sparrow.resources.feature_settings_control_planes_discovered
import org.jetbrains.compose.resources.stringResource

@Composable
fun ControlPlaneSettingsGroup(
    modifier: Modifier = Modifier,
    title: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = modifier.padding(horizontal = MaterialTheme.spacing.screenPadding)) {
        if (title != null) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = Alpha.OpaqueText),
                modifier = Modifier.padding(
                    start = MaterialTheme.spacing.base.div(2),
                    bottom = MaterialTheme.spacing.base
                )
            )
        }
        SparrowCardNoAnimation { Column(content = content) }
    }
}

@Composable
fun ControlPlaneSettingsDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = MaterialTheme.spacing.times(5)),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = Alpha.divider)
    )
}

@Composable
fun ControlPlaneSettingsEntry(
    entry: ControlPlaneUiModel,
    onRemove: () -> Unit
) {
    val statusColor = when (entry.status) {
        ControlPlaneUiStatus.ACTIVE, ControlPlaneUiStatus.AVAILABLE -> MaterialTheme.colorScheme.tertiary
        ControlPlaneUiStatus.UNREACHABLE -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val statusText = when (entry.status) {
        ControlPlaneUiStatus.ACTIVE -> stringResource(Res.string.feature_settings_control_plane_status_active)
        ControlPlaneUiStatus.AVAILABLE -> stringResource(Res.string.feature_settings_control_plane_status_available)
        ControlPlaneUiStatus.UNREACHABLE -> stringResource(Res.string.feature_settings_control_plane_status_unreachable)
        else -> stringResource(Res.string.feature_settings_control_plane_status_checking)
    }
    Row(
        modifier = Modifier.fillMaxWidth().padding(MaterialTheme.spacing.small),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(Dimens.SettingsScreen.primaryIconSize),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier.size(Dimens.ControlPlaneSettingsScreen.statusIndicatorSize)
                    .background(statusColor, CircleShape)
            )
        }
        Spacer(Modifier.size(MaterialTheme.spacing.small))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = entry.url,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = statusText,
                style = MaterialTheme.typography.bodySmall,
                color = statusColor
            )
            Text(
                text = if (entry.source == ControlPlaneUiSource.MANUAL) {
                    stringResource(Res.string.feature_settings_control_plane_source_manual)
                } else {
                    stringResource(Res.string.feature_settings_control_planes_discovered)
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (entry.canRemove && entry.source == ControlPlaneUiSource.MANUAL) {
            IconButton(onClick = onRemove) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = stringResource(Res.string.feature_settings_control_plane_remove),
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Preview
@Composable
private fun ControlPlaneSettingsGroupPreview() {
    SparrowTheme {
        ControlPlaneSettingsGroup(title = "Discovered Control Planes") {
            ControlPlaneSettingsEntry(
                entry = ControlPlaneUiModel(
                    url = "https://controlplane.example.com",
                    source = ControlPlaneUiSource.DIRECTORY,
                    canRemove = false,
                    status = ControlPlaneUiStatus.ACTIVE
                ),
                onRemove = {}
            )
            ControlPlaneSettingsDivider()
            ControlPlaneSettingsEntry(
                entry = ControlPlaneUiModel(
                    url = "https://manual.example.com",
                    source = ControlPlaneUiSource.MANUAL,
                    canRemove = true,
                    status = ControlPlaneUiStatus.AVAILABLE
                ),
                onRemove = {}
            )
        }
    }
}
