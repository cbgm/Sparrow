package com.cbgm.sparrow.feature.chats.presentation.direct.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.cbgm.sparrow.core.ui.component.SparrowAlertDialog
import com.cbgm.sparrow.core.ui.component.SparrowOutlinedButton
import com.cbgm.sparrow.core.ui.component.SparrowSecondaryButton
import com.cbgm.sparrow.core.ui.theme.SparrowTheme
import com.cbgm.sparrow.core.ui.theme.spacing
import com.cbgm.sparrow.resources.Res
import com.cbgm.sparrow.resources.base_cancel
import com.cbgm.sparrow.resources.feature_chats_import_contact_identity
import com.cbgm.sparrow.resources.feature_chats_manual_identity_setup_description
import com.cbgm.sparrow.resources.feature_chats_manual_identity_setup_title
import com.cbgm.sparrow.resources.feature_identity_share_my_identity
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun IdentitySetupDialog(
    onShareIdentity: () -> Unit,
    onImportIdentity: () -> Unit,
    onDismiss: () -> Unit
) {
    SparrowAlertDialog(
        onDismissRequest = onDismiss,
        title = stringResource(Res.string.feature_chats_manual_identity_setup_title),
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(text = stringResource(Res.string.feature_chats_manual_identity_setup_description))
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))
                SparrowOutlinedButton(
                    onClick = onShareIdentity,
                    modifier = Modifier.fillMaxWidth(),
                    text = stringResource(Res.string.feature_identity_share_my_identity)
                )
                SparrowOutlinedButton(
                    onClick = onImportIdentity,
                    modifier = Modifier.fillMaxWidth(),
                    text = stringResource(Res.string.feature_chats_import_contact_identity)
                )
            }
        },
        confirmButton = {},
        dismissButton = {
            SparrowSecondaryButton(
                onClick = onDismiss,
                text = stringResource(Res.string.base_cancel),
                fillMaxWidth = false
            )
        }
    )
}

@Preview
@Composable
private fun IdentitySetupDialogPreview() {
    SparrowTheme {
        IdentitySetupDialog(
            onShareIdentity = {},
            onImportIdentity = {},
            onDismiss = {}
        )
    }
}
