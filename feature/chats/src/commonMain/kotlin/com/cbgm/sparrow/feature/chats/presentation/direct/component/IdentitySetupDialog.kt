package com.cbgm.sparrow.feature.chats.presentation.direct.component

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.cbgm.sparrow.core.ui.component.SparrowAlertDialog
import com.cbgm.sparrow.core.ui.component.SparrowDialogAction
import com.cbgm.sparrow.core.ui.theme.Alpha
import com.cbgm.sparrow.core.ui.theme.Dimens
import com.cbgm.sparrow.core.ui.theme.SparrowTheme
import com.cbgm.sparrow.resources.Res
import com.cbgm.sparrow.resources.base_cancel
import com.cbgm.sparrow.resources.feature_chats_import_contact_identity
import com.cbgm.sparrow.resources.feature_chats_manual_identity_setup_description
import com.cbgm.sparrow.resources.feature_chats_manual_identity_setup_title
import com.cbgm.sparrow.resources.feature_identity_share_my_identity
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun IdentitySetupDialog(
    isVisible: Boolean,
    onShareIdentity: () -> Unit,
    onImportIdentity: () -> Unit,
    onDismiss: () -> Unit
) {
    SparrowAlertDialog(
        isVisible = isVisible,
        onDismissRequest = onDismiss,
        title = stringResource(Res.string.feature_chats_manual_identity_setup_title),
        text = {
            Text(text = stringResource(Res.string.feature_chats_manual_identity_setup_description))
        },
        confirmButton = {
            Column {
                SparrowDialogAction(
                    onClick = onShareIdentity,
                    text = stringResource(Res.string.feature_identity_share_my_identity)
                )
                HorizontalDivider(
                    thickness = Dimens.Base.dividerThickness,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = Alpha.divider)
                )
                SparrowDialogAction(
                    onClick = onImportIdentity,
                    text = stringResource(Res.string.feature_chats_import_contact_identity),
                    color = MaterialTheme.colorScheme.onSurface
                )
                HorizontalDivider(
                    thickness = Dimens.Base.dividerThickness,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = Alpha.divider)
                )
                SparrowDialogAction(
                    onClick = onDismiss,
                    text = stringResource(Res.string.base_cancel),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        dismissButton = {}
    )
}

@Preview
@Composable
private fun IdentitySetupDialogPreview() {
    SparrowTheme {
        IdentitySetupDialog(
            isVisible = true,
            onShareIdentity = {},
            onImportIdentity = {},
            onDismiss = {}
        )
    }
}
