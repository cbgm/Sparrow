package com.cbgm.sparrow.feature.onboarding.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cbgm.sparrow.feature.identity.presentation.platform.PhoneNumberHintLauncher
import com.cbgm.sparrow.feature.identity.presentation.platform.PhoneNumberHintResult
import com.cbgm.sparrow.feature.identity.presentation.setup.IdentityViewModel
import com.cbgm.sparrow.feature.identity.presentation.setup.model.IdentityUiEvent
import com.cbgm.sparrow.feature.identity.presentation.setup.model.IdentityUiState
import com.cbgm.sparrow.feature.onboarding.device.AutomaticPhoneNumberReader
import com.cbgm.sparrow.feature.onboarding.device.AutomaticPhoneNumberResult
import com.cbgm.sparrow.feature.onboarding.device.OnboardingPermissionRequester
import com.cbgm.sparrow.feature.onboarding.presentation.model.OnboardingPage
import com.cbgm.sparrow.feature.onboarding.presentation.model.OnboardingUiEvent
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun OnboardingRoute(
    onComplete: () -> Unit,
    viewModel: OnboardingViewModel = koinViewModel(),
    identityViewModel: IdentityViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val identityState by identityViewModel.uiState.collectAsStateWithLifecycle()
    var hintRequestId by remember { mutableIntStateOf(0) }

    OnboardingPermissionRequester(
        requestId = state.permissionRequestId,
        onResult = viewModel::onPermissionsResult
    )

    AutomaticPhoneNumberReader(
        requestId = state.automaticPhoneRequestId,
        enabled = state.page == OnboardingPage.PHONE && state.phonePermissionGranted,
        onResult = { result ->
            handleAutomaticPhoneNumberResult(result, identityViewModel)
        }
    )

    PhoneNumberHintLauncher(
        requestId = hintRequestId,
        enabled = state.page == OnboardingPage.PHONE,
        onResult = { result ->
            handlePhoneNumberHintResult(result, identityViewModel)
        }
    )

    LaunchedEffect(identityState) {
        when (identityState) {
            is IdentityUiState.Ready -> onComplete()
            IdentityUiState.Loading -> viewModel.setCreatingIdentity(true)
            else -> viewModel.setCreatingIdentity(false)
        }
    }

    OnboardingScreen(
        state = state,
        identityState = identityState,
        onUiEvent = { event ->
            handleOnboardingUiEvent(
                event = event,
                viewModel = viewModel,
                identityViewModel = identityViewModel,
                onChooseAnotherNumber = { hintRequestId += 1 }
            )
        }
    )
}

private fun handleOnboardingUiEvent(
    event: OnboardingUiEvent,
    viewModel: OnboardingViewModel,
    identityViewModel: IdentityViewModel,
    onChooseAnotherNumber: () -> Unit
) {
    when (event) {
        OnboardingUiEvent.ChooseAnotherNumberClicked -> onChooseAnotherNumber()
        is OnboardingUiEvent.PhoneNumberChanged ->
            identityViewModel.onUiEvent(IdentityUiEvent.PhoneNumberChanged(event.value))
        is OnboardingUiEvent.NameChanged ->
            identityViewModel.onUiEvent(IdentityUiEvent.NameChanged(event.value))
        OnboardingUiEvent.ApproveAndCreateClicked ->
            identityViewModel.onUiEvent(IdentityUiEvent.CreateIdentityClicked)
        else -> viewModel.onUiEvent(event)
    }
}

private fun handleAutomaticPhoneNumberResult(
    result: AutomaticPhoneNumberResult,
    identityViewModel: IdentityViewModel
) {
    when (result) {
        is AutomaticPhoneNumberResult.Found -> identityViewModel.onSuggestedPhoneNumber(result.phoneNumber)
        AutomaticPhoneNumberResult.Unavailable -> Unit
        is AutomaticPhoneNumberResult.Failed -> identityViewModel.onPhoneNumberHintFailed(result.message)
    }
}

private fun handlePhoneNumberHintResult(
    result: PhoneNumberHintResult,
    identityViewModel: IdentityViewModel
) {
    when (result) {
        is PhoneNumberHintResult.Selected -> identityViewModel.onSuggestedPhoneNumber(result.phoneNumber)
        PhoneNumberHintResult.Unavailable -> identityViewModel.onPhoneNumberHintUnavailable()
        PhoneNumberHintResult.Cancelled -> Unit
        is PhoneNumberHintResult.Failed -> identityViewModel.onPhoneNumberHintFailed(result.message)
    }
}
