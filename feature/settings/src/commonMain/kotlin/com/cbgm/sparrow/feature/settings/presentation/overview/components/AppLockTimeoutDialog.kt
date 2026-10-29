package com.cbgm.sparrow.feature.settings.presentation.overview.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.cbgm.sparrow.core.ui.component.SparrowAlertDialog
import com.cbgm.sparrow.core.ui.component.SparrowOutlinedButton
import com.cbgm.sparrow.core.ui.theme.Dimens
import com.cbgm.sparrow.core.ui.theme.spacing
import com.cbgm.sparrow.feature.applock.domain.model.AppLockTimeout
import com.cbgm.sparrow.resources.Res
import com.cbgm.sparrow.resources.base_cancel
import com.cbgm.sparrow.resources.feature_applock_timeout
import org.jetbrains.compose.resources.stringResource

@Composable
fun AppLockTimeoutDialog(
    currentTimeout: AppLockTimeout,
    label: @Composable (AppLockTimeout) -> String,
    onTimeoutSelected: (AppLockTimeout) -> Unit,
    onDismiss: () -> Unit
) {
    SparrowAlertDialog(
        isVisible = true,
        title = stringResource(Res.string.feature_applock_timeout),
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.base)) {
                AppLockTimeout.entries.forEach { timeout ->
                    val isSelected = timeout == currentTimeout

                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .background(
                                    color = if (isSelected) {
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
                                    } else {
                                        MaterialTheme.colorScheme.surfaceContainerLow
                                    },
                                    shape = MaterialTheme.shapes.small
                                )
                                .clickable { onTimeoutSelected(timeout) }
                                .padding(MaterialTheme.spacing.small),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = label(timeout),
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )

                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(Dimens.SettingsScreen.secondaryIconSize)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            SparrowOutlinedButton(
                onClick = onDismiss,
                fillMaxWidth = false,
                text = stringResource(Res.string.base_cancel)
            )
        },
        onDismissRequest = onDismiss
    )
}
