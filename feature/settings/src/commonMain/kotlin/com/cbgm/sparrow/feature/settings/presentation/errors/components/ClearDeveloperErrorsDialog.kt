package com.cbgm.sparrow.feature.settings.presentation.errors.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.cbgm.sparrow.core.ui.component.SparrowAlertDialog
import com.cbgm.sparrow.core.ui.component.SparrowDestructiveButton
import com.cbgm.sparrow.core.ui.component.SparrowOutlinedButton
import com.cbgm.sparrow.core.ui.theme.SparrowTheme
import com.cbgm.sparrow.resources.Res
import com.cbgm.sparrow.resources.base_cancel
import com.cbgm.sparrow.resources.feature_settings_clear_saved_errors
import com.cbgm.sparrow.resources.feature_settings_clear_saved_errors_description
import com.cbgm.sparrow.resources.feature_settings_clear_saved_errors_title
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun ClearDeveloperErrorsDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    SparrowAlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            SparrowDestructiveButton(
                onClick = onConfirm,
                fillMaxWidth = false,
                text = stringResource(Res.string.feature_settings_clear_saved_errors)
            )
        },
        dismissButton = {
            SparrowOutlinedButton(
                onClick = onDismiss,
                fillMaxWidth = false,
                text = stringResource(Res.string.base_cancel)
            )
        },
        title = stringResource(Res.string.feature_settings_clear_saved_errors_title),
        text = {
            Text(text = stringResource(Res.string.feature_settings_clear_saved_errors_description))
        }
    )
}

@Preview
@Composable
private fun ClearDeveloperErrorsDialogPreview() {
    SparrowTheme {
        ClearDeveloperErrorsDialog(
            onConfirm = {},
            onDismiss = {}
        )
    }
}
