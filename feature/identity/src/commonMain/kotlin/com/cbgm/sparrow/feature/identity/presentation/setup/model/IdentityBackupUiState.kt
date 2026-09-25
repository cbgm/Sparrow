package com.cbgm.sparrow.feature.identity.presentation.setup.model

enum class IdentityBackupUiStatus { NOT_BACKED_UP, EXPORTED, IMPORTED }

data class IdentityBackupUiState(
    val status: IdentityBackupUiStatus = IdentityBackupUiStatus.NOT_BACKED_UP,
    val busy: Boolean = false,
    val message: String? = null,
    val error: Boolean = false
)
