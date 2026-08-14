package com.cbgm.sparrow.feature.contactimport.presentation.component.verification

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.cbgm.sparrow.core.ui.component.SparrowAlertDialog
import com.cbgm.sparrow.core.ui.theme.SparrowTheme
import com.cbgm.sparrow.resources.Res
import com.cbgm.sparrow.resources.feature_contactimport_verifying_identity_qr
import org.jetbrains.compose.resources.stringResource

@Composable
fun QrVerificationProgressDialog() {
    SparrowAlertDialog(
        onDismissRequest = {},
        title = stringResource(Res.string.feature_contactimport_verifying_identity_qr),
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        confirmButton = {},
        dismissButton = {}
    )
}

@Preview
@Composable
private fun QrVerificationProgressDialogPreview() {
    SparrowTheme {
        QrVerificationProgressDialog()
    }
}
