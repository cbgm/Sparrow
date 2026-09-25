package com.cbgm.sparrow.feature.identity.presentation.setup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import com.cbgm.sparrow.core.ui.component.SparrowAlertDialog
import com.cbgm.sparrow.core.ui.component.SparrowApprovalButton
import com.cbgm.sparrow.core.ui.component.SparrowSecondaryButton
import com.cbgm.sparrow.core.ui.theme.spacing
import com.cbgm.sparrow.resources.Res
import com.cbgm.sparrow.resources.base_cancel
import com.cbgm.sparrow.resources.feature_identity_backup_export_action
import com.cbgm.sparrow.resources.feature_identity_backup_password
import com.cbgm.sparrow.resources.feature_identity_backup_restore_action
import com.cbgm.sparrow.resources.feature_identity_backup_restore_hint
import org.jetbrains.compose.resources.stringResource

/** Shared password prompt for both identity backup entry points; does not own the backup operation. */
@Composable
fun IdentityBackupPasswordDialog(
    isImport: Boolean,
    password: String,
    onPasswordChange: (String) -> Unit,
    busy: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val actionLabel = stringResource(
        if (isImport) {
            Res.string.feature_identity_backup_restore_action
        } else {
            Res.string.feature_identity_backup_export_action
        }
    )

    SparrowAlertDialog(
        onDismissRequest = onDismiss,
        title = actionLabel,
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium)) {
                if (isImport) {
                    Text(
                        text = stringResource(Res.string.feature_identity_backup_restore_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                OutlinedTextField(
                    value = password,
                    onValueChange = onPasswordChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(Res.string.feature_identity_backup_password)) },
                    shape = MaterialTheme.shapes.small,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.base)) {
                SparrowApprovalButton(
                    enabled = !busy && password.length >= (if (isImport) 1 else 12),
                    onClick = onConfirm,
                    modifier = Modifier.weight(1f),
                    text = actionLabel
                )
                SparrowSecondaryButton(
                    text = stringResource(Res.string.base_cancel),
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                )
            }
        },
        dismissButton = {},
        isVisible = true
    )
}
