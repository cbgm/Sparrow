package com.cbgm.sparrow.feature.identity.presentation.restore.component

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
import com.cbgm.sparrow.feature.identity.presentation.restore.model.IdentityRestoreUiState
import com.cbgm.sparrow.resources.Res
import com.cbgm.sparrow.resources.base_cancel
import com.cbgm.sparrow.resources.feature_identity_backup_password_label
import com.cbgm.sparrow.resources.feature_identity_backup_restore_action
import com.cbgm.sparrow.resources.feature_identity_backup_restore_hint
import org.jetbrains.compose.resources.stringResource

@Composable
fun IdentityRestoreDialog(
    state: IdentityRestoreUiState,
    onPasswordChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    SparrowAlertDialog(
        onDismissRequest = onDismiss,
        title = stringResource(Res.string.feature_identity_backup_restore_action),
        text = {
            Column {
                Text(stringResource(Res.string.feature_identity_backup_restore_hint))
                SparrowInputField(
                    value = state.password,
                    onValueChange = onPasswordChange,
                    label = stringResource(Res.string.feature_identity_backup_password_label),
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    isSingleLine = true
                )
            }
        },
        confirmButton = {
            SparrowDialogAction(
                enabled = state.canConfirm,
                onClick = onConfirm,
                text = stringResource(Res.string.feature_identity_backup_restore_action)
            )
        },
        dismissButton = {
            SparrowDialogAction(
                onClick = onDismiss,
                text = stringResource(Res.string.base_cancel),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        isVisible = state.isDialogVisible
    )
}
