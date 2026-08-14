package com.cbgm.sparrow.feature.contactimport.presentation.component.verification

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.cbgm.sparrow.core.ui.component.SparrowAlertDialog
import com.cbgm.sparrow.core.ui.component.SparrowApprovalButton
import com.cbgm.sparrow.core.ui.component.SparrowSecondaryButton
import com.cbgm.sparrow.core.ui.theme.SparrowTheme
import com.cbgm.sparrow.resources.Res
import com.cbgm.sparrow.resources.base_cancel
import com.cbgm.sparrow.resources.base_retry
import com.cbgm.sparrow.resources.feature_contactimport_qr_verification_failed
import org.jetbrains.compose.resources.stringResource

@Composable
fun QrVerificationErrorDialog(
    message: String,
    onRetry: () -> Unit,
    onCancel: () -> Unit
) {
    SparrowAlertDialog(
        onDismissRequest = {},
        title = stringResource(Res.string.feature_contactimport_qr_verification_failed),
        text = {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            SparrowApprovalButton(
                fillMaxWidth = false,
                onClick = onRetry,
                text = stringResource(Res.string.base_retry)
            )
        },
        dismissButton = {
            SparrowSecondaryButton(
                fillMaxWidth = false,
                onClick = onCancel,
                text = stringResource(Res.string.base_cancel)
            )
        }
    )
}

@Preview
@Composable
private fun QrVerificationErrorDialogPreview() {
    SparrowTheme {
        QrVerificationErrorDialog(
            message = "The scanned identity does not match this contact.",
            onRetry = {},
            onCancel = {}
        )
    }
}
