package com.cbgm.sparrow.feature.contactimport.presentation.scan.component

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.cbgm.sparrow.core.ui.component.SparrowAlertDialog
import com.cbgm.sparrow.core.ui.component.SparrowApprovalButton
import com.cbgm.sparrow.core.ui.component.SparrowSecondaryButton
import com.cbgm.sparrow.core.ui.theme.SparrowTheme
import com.cbgm.sparrow.resources.Res
import com.cbgm.sparrow.resources.base_back
import com.cbgm.sparrow.resources.feature_contactimport_camera_permission_required
import com.cbgm.sparrow.resources.feature_contactimport_grant_camera_permission
import com.cbgm.sparrow.resources.feature_contactimport_scan_identity
import org.jetbrains.compose.resources.stringResource

@Composable
fun CameraPermissionDialog(
    onGrantPermission: () -> Unit,
    onBack: () -> Unit
) {
    SparrowAlertDialog(
        onDismissRequest = onBack,
        title = stringResource(Res.string.feature_contactimport_scan_identity),
        text = {
            Text(
                text = stringResource(Res.string.feature_contactimport_camera_permission_required),
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            SparrowApprovalButton(
                fillMaxWidth = false,
                onClick = onGrantPermission,
                text = stringResource(Res.string.feature_contactimport_grant_camera_permission)
            )
        },
        dismissButton = {
            SparrowSecondaryButton(
                fillMaxWidth = false,
                onClick = onBack,
                text = stringResource(Res.string.base_back)
            )
        }
    )
}

@Preview
@Composable
private fun CameraPermissionDialogPreview() {
    SparrowTheme {
        CameraPermissionDialog(
            onGrantPermission = {},
            onBack = {}
        )
    }
}
