package com.cbgm.sparrow.feature.onboarding.presentation.model

sealed interface OnboardingUiEvent {
    data object NextClicked : OnboardingUiEvent

    data object RequestPermissionsClicked : OnboardingUiEvent

    data object ChooseAnotherNumberClicked : OnboardingUiEvent

    data object RetryAutomaticNumberClicked : OnboardingUiEvent
}
