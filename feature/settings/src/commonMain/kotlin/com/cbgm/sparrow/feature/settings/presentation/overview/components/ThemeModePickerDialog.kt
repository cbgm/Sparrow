package com.cbgm.sparrow.feature.settings.presentation.overview.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.cbgm.sparrow.core.ui.component.SparrowAlertDialog
import com.cbgm.sparrow.core.ui.component.SparrowDialogAction
import com.cbgm.sparrow.core.ui.component.SparrowRadio
import com.cbgm.sparrow.core.ui.theme.AppThemeMode
import com.cbgm.sparrow.core.ui.theme.spacing
import com.cbgm.sparrow.resources.Res
import com.cbgm.sparrow.resources.base_cancel
import com.cbgm.sparrow.resources.feature_settings_theme
import com.cbgm.sparrow.resources.feature_settings_theme_dark
import com.cbgm.sparrow.resources.feature_settings_theme_light
import com.cbgm.sparrow.resources.feature_settings_theme_system
import com.cbgm.sparrow.resources.feature_settings_theme_system_description
import org.jetbrains.compose.resources.stringResource

@Composable
fun ThemeModePickerDialog(
    currentMode: AppThemeMode,
    onModeSelected: (AppThemeMode) -> Unit,
    onDismiss: () -> Unit
) {
    SparrowAlertDialog(
        isVisible = true,
        title = stringResource(Res.string.feature_settings_theme),
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)) {
                AppThemeMode.entries.forEach { mode ->
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .clickable { onModeSelected(mode) },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
                    ) {
                        SparrowRadio(selected = mode == currentMode, onClick = null)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = themeModeTitle(mode),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            themeModeDescription(mode)?.let { description ->
                                Text(
                                    text = description,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            SparrowDialogAction(
                onClick = onDismiss,
                text = stringResource(Res.string.base_cancel),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        onDismissRequest = onDismiss
    )
}

@Composable
fun themeModeTitle(mode: AppThemeMode): String =
    when (mode) {
        AppThemeMode.SYSTEM -> stringResource(Res.string.feature_settings_theme_system)
        AppThemeMode.LIGHT -> stringResource(Res.string.feature_settings_theme_light)
        AppThemeMode.DARK -> stringResource(Res.string.feature_settings_theme_dark)
    }

@Composable
fun themeModeDescription(mode: AppThemeMode): String? =
    when (mode) {
        AppThemeMode.SYSTEM -> stringResource(Res.string.feature_settings_theme_system_description)
        AppThemeMode.LIGHT, AppThemeMode.DARK -> null
    }
