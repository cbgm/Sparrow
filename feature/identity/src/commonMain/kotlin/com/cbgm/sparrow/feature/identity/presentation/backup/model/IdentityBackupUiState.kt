package com.cbgm.sparrow.feature.identity.presentation.backup.model

enum class BackupStateUi {
    NOT_BACKED_UP,
    EXPORTED,
    IMPORTED
}

data class IdentityBackupUiState(
    val status: BackupStateUi = BackupStateUi.NOT_BACKED_UP,
    val busy: Boolean = false,
    val message: String? = null,
    val error: Boolean = false
)
