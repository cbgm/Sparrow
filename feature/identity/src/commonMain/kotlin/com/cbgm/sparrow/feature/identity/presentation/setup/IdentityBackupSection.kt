package com.cbgm.sparrow.feature.identity.presentation.setup

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.cbgm.sparrow.core.ui.component.SparrowApprovalButton
import com.cbgm.sparrow.core.ui.theme.Dimens
import com.cbgm.sparrow.core.ui.theme.spacing
import com.cbgm.sparrow.feature.identity.presentation.setup.model.IdentityBackupUiState
import com.cbgm.sparrow.feature.identity.presentation.setup.model.IdentityBackupUiStatus
import com.cbgm.sparrow.resources.Res
import com.cbgm.sparrow.resources.feature_identity_backup_export_action
import com.cbgm.sparrow.resources.feature_identity_backup_exported
import com.cbgm.sparrow.resources.feature_identity_backup_imported
import com.cbgm.sparrow.resources.feature_identity_backup_warning
import org.jetbrains.compose.resources.stringResource

@Composable
fun IdentityBackupSection(state: IdentityBackupUiState, onExport: () -> Unit) {
    val description = when (state.status) {
        IdentityBackupUiStatus.NOT_BACKED_UP -> Res.string.feature_identity_backup_warning
        IdentityBackupUiStatus.EXPORTED -> Res.string.feature_identity_backup_exported
        IdentityBackupUiStatus.IMPORTED -> Res.string.feature_identity_backup_imported
    }
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(description),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(MaterialTheme.spacing.medium))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
        Spacer(Modifier.height(MaterialTheme.spacing.medium))
        SparrowApprovalButton(
            onClick = onExport,
            enabled = !state.busy,
            content = {
                if (state.busy) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .padding(end = MaterialTheme.spacing.small)
                            .size(Dimens.Button.loadingIndicatorSize),
                        strokeWidth = Dimens.Button.loadingIndicatorStrokeWidth,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = stringResource(Res.string.feature_identity_backup_export_action),
                    style = MaterialTheme.typography.labelLarge
                )
            }
        )
        state.message?.let { message ->
            Spacer(Modifier.height(MaterialTheme.spacing.small))
            Text(
                text = message,
                color = if (state.error) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
