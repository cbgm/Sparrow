package com.cbgm.sparrow.feature.settings.presentation.errors.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import com.cbgm.sparrow.core.ui.component.SparrowAlertDialog
import com.cbgm.sparrow.core.ui.component.SparrowDestructiveButton
import com.cbgm.sparrow.core.ui.component.SparrowOutlinedButton
import com.cbgm.sparrow.core.ui.theme.Dimens
import com.cbgm.sparrow.core.ui.theme.SparrowTheme
import com.cbgm.sparrow.core.ui.theme.spacing
import com.cbgm.sparrow.resources.Res
import com.cbgm.sparrow.resources.base_cancel
import com.cbgm.sparrow.resources.feature_settings_clear_saved_errors
import com.cbgm.sparrow.resources.feature_settings_clear_saved_errors_description
import com.cbgm.sparrow.resources.feature_settings_clear_saved_errors_title
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun ClearDeveloperErrorsDialog(
    isVisible: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    SparrowAlertDialog(
        isVisible = isVisible,
        onDismissRequest = onDismiss,
        confirmButton = {
            Row {
                SparrowDestructiveButton(
                    onClick = onConfirm,
                    modifier = Modifier.weight(1f),
                    text = stringResource(Res.string.feature_settings_clear_saved_errors)
                )
                Spacer(Modifier.width(MaterialTheme.spacing.base))
                SparrowOutlinedButton(
                    onClick = onDismiss,
                    fillMaxWidth = false,
                    text = stringResource(Res.string.base_cancel)
                )
            }
        },
        dismissButton = {},
        title = stringResource(Res.string.feature_settings_clear_saved_errors_title),
        text = {
            Text(
                text = stringResource(Res.string.feature_settings_clear_saved_errors_description),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.large)
                    .background(MaterialTheme.colorScheme.errorContainer)
                    .border(
                        Dimens.Base.borderStrokeWidth,
                        MaterialTheme.colorScheme.error.copy(alpha = 0.35f),
                        MaterialTheme.shapes.large
                    )
                    .padding(MaterialTheme.spacing.medium),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
        }
    )
}

@Preview
@Composable
private fun ClearDeveloperErrorsDialogPreview() {
    SparrowTheme {
        ClearDeveloperErrorsDialog(
            isVisible = true,
            onConfirm = {},
            onDismiss = {}
        )
    }
}
