package com.cbgm.sparrow.feature.identity.presentation.verification

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cbgm.sparrow.feature.identity.device.PhoneVerificationLauncher
import com.cbgm.sparrow.feature.identity.presentation.verification.model.PhoneVerificationStage
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun PhoneVerificationRoute(
    phoneNumber: String?,
    onContinue: () -> Unit,
    onCancel: () -> Unit,
    viewModel: PhoneVerificationViewModel = koinViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(phoneNumber) {
        if (phoneNumber == null) viewModel.cancel() else viewModel.start(phoneNumber)
    }
    LaunchedEffect(state.stage, phoneNumber) {
        if (phoneNumber != null && state.stage == PhoneVerificationStage.VERIFIED) {
            viewModel.cancel()
            onContinue()
        }
    }
    PhoneVerificationLauncher(request = state.request, onResult = viewModel::onResult)
    PhoneVerificationDialog(
        state = state,
        onCodeChanged = viewModel::codeChanged,
        onSendCode = viewModel::sendCode,
        onConfirm = viewModel::confirm,
        onRetry = viewModel::retry,
        onSkip = viewModel::skip,
        onCancel = {
            viewModel.cancel()
            onCancel()
        },
        isVisible = phoneNumber != null
    )
}
