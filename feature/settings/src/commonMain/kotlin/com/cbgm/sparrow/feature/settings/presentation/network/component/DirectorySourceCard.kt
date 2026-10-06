package com.cbgm.sparrow.feature.settings.presentation.network.component

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.cbgm.sparrow.core.ui.theme.Dimens
import com.cbgm.sparrow.core.ui.theme.SparrowTheme
import com.cbgm.sparrow.core.ui.theme.spacing
import com.cbgm.sparrow.feature.settings.presentation.network.model.ControlPlaneDirectoryError
import com.cbgm.sparrow.resources.Res
import com.cbgm.sparrow.resources.feature_settings_control_plane_directory_disabled
import com.cbgm.sparrow.resources.feature_settings_control_plane_directory_edit
import com.cbgm.sparrow.resources.feature_settings_control_plane_directory_sync_failed
import com.cbgm.sparrow.resources.feature_settings_control_plane_directory_title
import com.cbgm.sparrow.resources.feature_settings_control_plane_json_unverified
import com.cbgm.sparrow.resources.feature_settings_control_plane_remove
import org.jetbrains.compose.resources.stringResource

@Composable
fun DirectorySourceCard(
    directoryUrl: String,
    jsonDirectoryUrl: String,
    directoryError: ControlPlaneDirectoryError?,
    directoryFailureDetail: String?,
    onEditDirectory: () -> Unit,
    onRemoveDirectory: () -> Unit,
    modifier: Modifier = Modifier,
    isSyncing: Boolean = false
) {
    ControlPlaneSettingsGroup(
        modifier = modifier,
        title = stringResource(Res.string.feature_settings_control_plane_directory_title)
    ) {
        val directoryAddress = directoryUrl.ifBlank { jsonDirectoryUrl }
        Row(
            modifier = Modifier.fillMaxWidth().padding(MaterialTheme.spacing.small),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Cloud,
                contentDescription = null,
                modifier = Modifier
                    .size(Dimens.SettingsScreen.primaryIconSize)
                    .infiniteFlashing(enabled = isSyncing),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.size(MaterialTheme.spacing.small))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = directoryAddress.ifBlank {
                        stringResource(Res.string.feature_settings_control_plane_directory_disabled)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1
                )
                if (jsonDirectoryUrl.isNotBlank() && directoryUrl.isBlank()) {
                    Text(
                        text = stringResource(Res.string.feature_settings_control_plane_json_unverified),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (directoryError != null) {
                    Text(
                        text = stringResource(Res.string.feature_settings_control_plane_directory_sync_failed),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                    directoryFailureDetail?.let { detail ->
                        Text(
                            text = detail,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
            IconButton(onClick = onEditDirectory) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = stringResource(Res.string.feature_settings_control_plane_directory_edit)
                )
            }
            if (directoryAddress.isNotBlank()) {
                IconButton(onClick = onRemoveDirectory) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = stringResource(Res.string.feature_settings_control_plane_remove),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
private fun Modifier.infiniteFlashing(
    enabled: Boolean,
    durationMillis: Int = 1000
): Modifier {
    if (!enabled) return this

    val infiniteTransition = rememberInfiniteTransition(label = "InfiniteFlashingTransition")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = durationMillis, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "InfiniteFlashingAlpha"
    )

    return this.graphicsLayer {
        this.alpha = alpha
    }
}

@Preview
@Composable
private fun DirectorySourceCardPreview() {
    SparrowTheme {
        DirectorySourceCard(
            directoryUrl = "https://directory.sparrow.com",
            jsonDirectoryUrl = "",
            directoryError = null,
            directoryFailureDetail = null,
            onEditDirectory = {},
            onRemoveDirectory = {}
        )
    }
}
