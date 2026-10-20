package com.cbgm.sparrow.feature.identity.presentation.backup.mapper

import com.cbgm.sparrow.feature.identity.domain.model.IdentityBackupState
import com.cbgm.sparrow.feature.identity.presentation.backup.model.BackupStateUi

internal fun IdentityBackupState.toUi(): BackupStateUi = when (this) {
    IdentityBackupState.NOT_BACKED_UP -> BackupStateUi.NOT_BACKED_UP
    IdentityBackupState.EXPORTED -> BackupStateUi.EXPORTED
    IdentityBackupState.IMPORTED -> BackupStateUi.IMPORTED
}
