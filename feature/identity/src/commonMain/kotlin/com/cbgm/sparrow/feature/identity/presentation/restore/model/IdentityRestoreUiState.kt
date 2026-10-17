package com.cbgm.sparrow.feature.identity.presentation.restore.model

data class IdentityRestoreUiState(
    val stage: IdentityRestoreStageUi = IdentityRestoreStageUi.IDLE,
    val password: String = "",
    val phoneVerificationPending: Boolean = false
) {
    val isDialogVisible: Boolean
        get() = !phoneVerificationPending &&
            (stage == IdentityRestoreStageUi.PASSWORD || stage == IdentityRestoreStageUi.RESTORING)
    val canConfirm: Boolean
        get() = stage == IdentityRestoreStageUi.PASSWORD && password.isNotEmpty()

    override fun toString(): String = "IdentityRestoreUiState(stage=$stage)"
}
