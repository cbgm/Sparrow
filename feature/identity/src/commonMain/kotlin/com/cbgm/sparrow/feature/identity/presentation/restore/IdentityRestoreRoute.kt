package com.cbgm.sparrow.feature.identity.presentation.restore

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cbgm.sparrow.feature.identity.presentation.restore.component.IdentityRestoreDialog
import com.cbgm.sparrow.feature.identity.presentation.restore.model.IdentityRestoreUiEvent
import com.cbgm.sparrow.feature.identity.presentation.verification.PhoneVerificationRoute
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun IdentityRestoreRoute(viewModel: IdentityRestoreViewModel = koinViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LifecycleResumeEffect(state.stage) {
        viewModel.finish()
        onPauseOrDispose { }
    }

    PhoneVerificationRoute(
        phoneNumber = viewModel.verificationPhoneNumber,
        onContinue = viewModel::onVerificationContinued,
        onCancel = viewModel::onVerificationCancelled
    )

    IdentityRestoreDialog(
        state = state,
        onPasswordChange = { viewModel.onUiEvent(IdentityRestoreUiEvent.PasswordChanged(it)) },
        onConfirm = { viewModel.onUiEvent(IdentityRestoreUiEvent.ConfirmClicked) },
        onDismiss = { viewModel.onUiEvent(IdentityRestoreUiEvent.DismissClicked) }
    )
}
