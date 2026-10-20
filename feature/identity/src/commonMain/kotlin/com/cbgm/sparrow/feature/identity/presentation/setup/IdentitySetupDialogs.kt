package com.cbgm.sparrow.feature.identity.presentation.setup

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cbgm.sparrow.feature.identity.presentation.create.IdentityCreateViewModel
import com.cbgm.sparrow.feature.identity.presentation.restore.IdentityRestoreViewModel
import com.cbgm.sparrow.feature.identity.presentation.restore.component.IdentityRestoreDialog
import com.cbgm.sparrow.feature.identity.presentation.restore.model.IdentityRestoreStageUi
import com.cbgm.sparrow.feature.identity.presentation.restore.model.IdentityRestoreUiEvent
import com.cbgm.sparrow.feature.identity.presentation.verification.component.PhoneVerificationDialog

/**
 * The restore and phone verification dialogs of the identity setup, placed on top of the
 * screen that hosts "Approve and create" and "Restore". Both flows share one verification.
 */
@Composable
fun IdentitySetupDialogs(
    createViewModel: IdentityCreateViewModel,
    restoreViewModel: IdentityRestoreViewModel
) {
    val restoreState by restoreViewModel.uiState.collectAsStateWithLifecycle()
    val createPhone by createViewModel.phoneToVerify.collectAsStateWithLifecycle()
    val restorePhone by restoreViewModel.phoneToVerify.collectAsStateWithLifecycle()

    LaunchedEffect(restoreState.stage) {
        if (restoreState.stage == IdentityRestoreStageUi.COMPLETED) createViewModel.refreshIdentity()
        restoreViewModel.finish()
    }

    IdentityRestoreDialog(
        state = restoreState,
        onPasswordChange = {
            restoreViewModel.onUiEvent(IdentityRestoreUiEvent.PasswordChanged(it))
        },
        onConfirm = { restoreViewModel.onUiEvent(IdentityRestoreUiEvent.ConfirmClicked) },
        onDismiss = { restoreViewModel.onUiEvent(IdentityRestoreUiEvent.DismissClicked) }
    )
    PhoneVerificationDialog(
        phoneNumber = createPhone ?: restorePhone,
        onResult = { result ->
            if (createPhone != null) {
                createViewModel.onPhoneVerification(result)
            } else {
                restoreViewModel.onPhoneVerification(result)
            }
        }
    )
}
