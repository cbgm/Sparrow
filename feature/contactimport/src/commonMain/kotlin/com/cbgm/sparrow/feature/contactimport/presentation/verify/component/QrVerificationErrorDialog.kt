package com.cbgm.sparrow.feature.contactimport.presentation.verify.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import com.cbgm.sparrow.core.ui.component.SparrowAlertDialog
import com.cbgm.sparrow.core.ui.component.SparrowDialogAction
import com.cbgm.sparrow.core.ui.theme.Dimens
import com.cbgm.sparrow.core.ui.theme.SparrowTheme
import com.cbgm.sparrow.core.ui.theme.spacing
import com.cbgm.sparrow.resources.Res
import com.cbgm.sparrow.resources.base_cancel
import com.cbgm.sparrow.resources.base_retry
import com.cbgm.sparrow.resources.feature_contactimport_qr_verification_failed
import org.jetbrains.compose.resources.stringResource

@Composable
fun QrVerificationErrorDialog(
    isVisible: Boolean,
    message: String,
    onRetry: () -> Unit,
    onCancel: () -> Unit
) {
    SparrowAlertDialog(
        isVisible = isVisible,
        onDismissRequest = {},
        title = stringResource(Res.string.feature_contactimport_qr_verification_failed),
        text = {
            Text(
                text = message,
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
        },
        confirmButton = {
            SparrowDialogAction(
                onClick = onRetry,
                text = stringResource(Res.string.base_retry)
            )
        },
        dismissButton = {
            SparrowDialogAction(
                onClick = onCancel,
                text = stringResource(Res.string.base_cancel),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    )
}

@Preview
@Composable
private fun QrVerificationErrorDialogPreview() {
    SparrowTheme {
        QrVerificationErrorDialog(
            isVisible = true,
            message = "The scanned identity does not match this contact.",
            onRetry = {},
            onCancel = {}
        )
    }
}
