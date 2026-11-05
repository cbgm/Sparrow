package com.cbgm.sparrow.feature.identity.presentation.repair.model

enum class IdentityRepairStageUi {
    IDLE,
    SELECTING,
    READING,
    PASSWORD,
    REPAIRING,
    CONFIRM_RESET,
    RESETTING,
    FINISHED
}

data class IdentityRepairUiState(
    val stage: IdentityRepairStageUi = IdentityRepairStageUi.IDLE,
    val password: String = ""
) {
    val isBusy: Boolean
        get() = stage != IdentityRepairStageUi.IDLE && stage != IdentityRepairStageUi.CONFIRM_RESET

    val isPasswordDialogVisible: Boolean
        get() = stage == IdentityRepairStageUi.PASSWORD || stage == IdentityRepairStageUi.REPAIRING

    val canConfirmPassword: Boolean
        get() = stage == IdentityRepairStageUi.PASSWORD && password.isNotEmpty()

    override fun toString(): String = "IdentityRepairUiState(stage=$stage)"
}
