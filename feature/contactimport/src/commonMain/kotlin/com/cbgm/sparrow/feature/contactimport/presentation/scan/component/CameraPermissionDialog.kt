package com.cbgm.sparrow.feature.contactimport.presentation.scan.component

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
import com.cbgm.sparrow.core.ui.component.SparrowApprovalButton
import com.cbgm.sparrow.core.ui.component.SparrowSecondaryButton
import com.cbgm.sparrow.core.ui.theme.Dimens
import com.cbgm.sparrow.core.ui.theme.SparrowTheme
import com.cbgm.sparrow.core.ui.theme.spacing
import com.cbgm.sparrow.resources.Res
import com.cbgm.sparrow.resources.base_back
import com.cbgm.sparrow.resources.feature_contactimport_camera_permission_required
import com.cbgm.sparrow.resources.feature_contactimport_grant_camera_permission
import com.cbgm.sparrow.resources.feature_contactimport_scan_identity
import org.jetbrains.compose.resources.stringResource

@Composable
fun CameraPermissionDialog(
    isVisible: Boolean,
    onGrantPermission: () -> Unit,
    onBack: () -> Unit
) {
    SparrowAlertDialog(
        isVisible = isVisible,
        onDismissRequest = onBack,
        title = stringResource(Res.string.feature_contactimport_scan_identity),
        text = {
            Text(
                text = stringResource(Res.string.feature_contactimport_camera_permission_required),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.large)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .border(
                        Dimens.Base.borderStrokeWidth,
                        MaterialTheme.colorScheme.outlineVariant,
                        MaterialTheme.shapes.large
                    )
                    .padding(MaterialTheme.spacing.medium),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
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
            isVisible = true,
            onGrantPermission = {},
            onBack = {}
        )
    }
}
