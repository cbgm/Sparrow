package com.cbgm.sparrow.feature.identity.presentation.backup

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.cbgm.sparrow.core.ui.component.SparrowApprovalButton
import com.cbgm.sparrow.core.ui.component.SparrowBanner
import com.cbgm.sparrow.core.ui.icon.SparrowIcons
import com.cbgm.sparrow.core.ui.theme.Dimens
import com.cbgm.sparrow.core.ui.theme.spacing
import com.cbgm.sparrow.feature.identity.presentation.backup.model.BackupStateUi
import com.cbgm.sparrow.feature.identity.presentation.backup.model.IdentityBackupUiState
import com.cbgm.sparrow.resources.Res
import com.cbgm.sparrow.resources.feature_identity_backup_export_action
import com.cbgm.sparrow.resources.feature_identity_backup_exported
import com.cbgm.sparrow.resources.feature_identity_backup_imported
import com.cbgm.sparrow.resources.feature_identity_backup_status_exported
import com.cbgm.sparrow.resources.feature_identity_backup_status_imported
import com.cbgm.sparrow.resources.feature_identity_backup_status_missing
import com.cbgm.sparrow.resources.feature_identity_backup_warning
import org.jetbrains.compose.resources.stringResource

@Composable
fun IdentityBackupSection(state: IdentityBackupUiState, onExport: () -> Unit) {
    val isBackedUp = state.status != BackupStateUi.NOT_BACKED_UP
    Column(modifier = Modifier.fillMaxWidth()) {
        SparrowBanner(
            icon = if (isBackedUp) SparrowIcons.ShieldCheck else Icons.Default.WarningAmber,
            accent = if (isBackedUp) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error,
            title = stringResource(
                when (state.status) {
                    BackupStateUi.NOT_BACKED_UP -> Res.string.feature_identity_backup_status_missing
                    BackupStateUi.EXPORTED -> Res.string.feature_identity_backup_status_exported
                    BackupStateUi.IMPORTED -> Res.string.feature_identity_backup_status_imported
                }
            ),
            description = stringResource(
                when (state.status) {
                    BackupStateUi.NOT_BACKED_UP -> Res.string.feature_identity_backup_warning
                    BackupStateUi.EXPORTED -> Res.string.feature_identity_backup_exported
                    BackupStateUi.IMPORTED -> Res.string.feature_identity_backup_imported
                }
            )
        )
        Spacer(Modifier.height(MaterialTheme.spacing.large))
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
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
                Text(
                    text = stringResource(Res.string.feature_identity_backup_export_action),
                    style = MaterialTheme.typography.bodyMedium
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
