package com.cbgm.sparrow.feature.identity.presentation.backup.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import com.cbgm.sparrow.core.ui.component.SparrowAlertDialog
import com.cbgm.sparrow.core.ui.component.SparrowDialogAction
import com.cbgm.sparrow.core.ui.component.SparrowInputField
import com.cbgm.sparrow.core.ui.theme.spacing
import com.cbgm.sparrow.resources.Res
import com.cbgm.sparrow.resources.base_cancel
import com.cbgm.sparrow.resources.feature_identity_backup_export_action
import com.cbgm.sparrow.resources.feature_identity_backup_password_hint
import com.cbgm.sparrow.resources.feature_identity_backup_password_label
import org.jetbrains.compose.resources.stringResource

@Composable
fun IdentityBackupPasswordDialog(
    password: String,
    onPasswordChange: (String) -> Unit,
    busy: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    isVisible: Boolean
) {
    SparrowAlertDialog(
        onDismissRequest = onDismiss,
        title = stringResource(Res.string.feature_identity_backup_export_action),
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.micro)) {
                SparrowInputField(
                    value = password,
                    onValueChange = onPasswordChange,
                    label = stringResource(Res.string.feature_identity_backup_password_label),
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    isSingleLine = true
                )
                Text(
                    text = stringResource(Res.string.feature_identity_backup_password_hint),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            SparrowDialogAction(
                enabled = !busy && password.length >= 12,
                onClick = onConfirm,
                text = stringResource(Res.string.feature_identity_backup_export_action)
            )
        },
        dismissButton = {
            SparrowDialogAction(
                text = stringResource(Res.string.base_cancel),
                onClick = onDismiss,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        isVisible = isVisible
    )
}
