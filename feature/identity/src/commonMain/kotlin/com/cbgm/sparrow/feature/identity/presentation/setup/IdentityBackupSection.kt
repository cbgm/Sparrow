package com.cbgm.sparrow.feature.identity.presentation.setup

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.cbgm.sparrow.core.ui.component.SparrowCard
import com.cbgm.sparrow.core.ui.component.SparrowSecondaryButton
import com.cbgm.sparrow.core.ui.theme.spacing
import com.cbgm.sparrow.feature.identity.presentation.setup.model.IdentityBackupUiState
import com.cbgm.sparrow.feature.identity.presentation.setup.model.IdentityBackupUiStatus
import com.cbgm.sparrow.resources.Res
import com.cbgm.sparrow.resources.feature_identity_backup_export_action
import com.cbgm.sparrow.resources.feature_identity_backup_exported
import com.cbgm.sparrow.resources.feature_identity_backup_imported
import com.cbgm.sparrow.resources.feature_identity_backup_title
import com.cbgm.sparrow.resources.feature_identity_backup_warning
import org.jetbrains.compose.resources.stringResource

@Composable
fun IdentityBackupSection(state: IdentityBackupUiState, onExport: () -> Unit) {
    SparrowCard {
        Column(modifier = Modifier.padding(MaterialTheme.spacing.medium)) {
            Text(stringResource(Res.string.feature_identity_backup_title), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(MaterialTheme.spacing.small))
            val description = when (state.status) {
                IdentityBackupUiStatus.NOT_BACKED_UP -> Res.string.feature_identity_backup_warning
                IdentityBackupUiStatus.EXPORTED -> Res.string.feature_identity_backup_exported
                IdentityBackupUiStatus.IMPORTED -> Res.string.feature_identity_backup_imported
            }
            Text(stringResource(description), style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(MaterialTheme.spacing.medium))
            SparrowSecondaryButton(
                onClick = onExport,
                enabled = !state.busy,
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(Res.string.feature_identity_backup_export_action)
            )
            state.message?.let {
                Spacer(Modifier.height(MaterialTheme.spacing.small))
                Text(
                    it,
                    color = if (state.error) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}
