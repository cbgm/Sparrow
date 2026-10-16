package com.cbgm.sparrow.feature.identity.presentation.backup.model

sealed interface IdentityBackupUiEvent {
    data object BackClicked : IdentityBackupUiEvent

    data object ExportIdentityClicked : IdentityBackupUiEvent

    data object RestoreIdentityClicked : IdentityBackupUiEvent

    data class PrepareBackup(
        val password: CharArray
    ) : IdentityBackupUiEvent

    data class RestoreBackup(
        val document: ByteArray,
        val password: CharArray
    ) : IdentityBackupUiEvent

    data class BackupFileWritten(
        val success: Boolean,
        val failure: String?
    ) : IdentityBackupUiEvent
}
