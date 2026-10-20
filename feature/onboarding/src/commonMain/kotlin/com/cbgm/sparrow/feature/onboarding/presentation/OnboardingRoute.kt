package com.cbgm.sparrow.feature.onboarding.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cbgm.sparrow.feature.identity.device.PhoneNumberHintLauncher
import com.cbgm.sparrow.feature.identity.device.PhoneNumberHintResult
import com.cbgm.sparrow.feature.identity.presentation.create.IdentityCreateViewModel
import com.cbgm.sparrow.feature.identity.presentation.create.model.IdentityCreateUiState
import com.cbgm.sparrow.feature.identity.presentation.restore.IdentityRestoreViewModel
import com.cbgm.sparrow.feature.identity.presentation.setup.IdentitySetupDialogs
import com.cbgm.sparrow.feature.onboarding.device.AutomaticPhoneNumberReader
import com.cbgm.sparrow.feature.onboarding.device.AutomaticPhoneNumberResult
import com.cbgm.sparrow.feature.onboarding.device.OnboardingPermissionRequester
import com.cbgm.sparrow.feature.onboarding.presentation.model.OnboardingPage
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun OnboardingRoute(
    onComplete: () -> Unit,
    viewModel: OnboardingViewModel = koinViewModel(),
    identityViewModel: IdentityCreateViewModel = koinViewModel(),
    restoreViewModel: IdentityRestoreViewModel = koinViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val identityState by identityViewModel.uiState.collectAsStateWithLifecycle()

    LifecycleResumeEffect(identityViewModel) {
        identityViewModel.refreshIdentity()
        onPauseOrDispose { }
    }
    LaunchedEffect(identityState) {
        if (identityState == IdentityCreateUiState.Completed) onComplete()
    }

    OnboardingPermissionRequester(
        requestId = state.permissionRequestId,
        onResult = viewModel::onPermissionsResult
    )
    AutomaticPhoneNumberReader(
        requestId = state.automaticPhoneRequestId,
        enabled = state.page == OnboardingPage.PHONE && state.phonePermissionGranted,
        onResult = { result ->
            when (result) {
                is AutomaticPhoneNumberResult.Found -> identityViewModel.onSuggestedPhoneNumber(result.phoneNumber)
                is AutomaticPhoneNumberResult.Failed -> identityViewModel.onPhoneNumberHintFailed()
                AutomaticPhoneNumberResult.Unavailable -> Unit
            }
        }
    )
    PhoneNumberHintLauncher(
        requestId = state.phoneNumberHintRequestId,
        enabled = state.page == OnboardingPage.PHONE,
        onResult = { result ->
            when (result) {
                is PhoneNumberHintResult.Selected -> identityViewModel.onSuggestedPhoneNumber(result.phoneNumber)
                is PhoneNumberHintResult.Failed -> identityViewModel.onPhoneNumberHintFailed()
                PhoneNumberHintResult.Unavailable, PhoneNumberHintResult.Cancelled -> Unit
            }
        }
    )

    OnboardingScreen(
        state = state,
        identityState = identityState,
        onRestoreIdentity = {
            (identityState as? IdentityCreateUiState.Form)?.let { form ->
                restoreViewModel.start(phoneNumber = form.phoneNumber, name = form.name)
            }
        },
        onIdentityUiEvent = identityViewModel::onUiEvent,
        onUiEvent = viewModel::onUiEvent
    )
    IdentitySetupDialogs(createViewModel = identityViewModel, restoreViewModel = restoreViewModel)
}
