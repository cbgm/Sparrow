package com.cbgm.sparrow.feature.identity.presentation.backup.model

sealed interface IdentityBackupUiEvent {
    data object BackClicked : IdentityBackupUiEvent

    data object ExportIdentityClicked : IdentityBackupUiEvent

    data class PrepareBackup(
        val password: CharArray
    ) : IdentityBackupUiEvent {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as PrepareBackup

            if (!password.contentEquals(other.password)) return false

            return true
        }

        override fun hashCode(): Int = password.contentHashCode()
    }

    data class BackupFileWritten(
        val success: Boolean,
        val failure: String?
    ) : IdentityBackupUiEvent
}
