package com.cbgm.sparrow.feature.onboarding.presentation.model

sealed interface OnboardingUiEvent {
    data object NextClicked : OnboardingUiEvent

    data object RequestPermissionsClicked : OnboardingUiEvent

    data object ChooseAnotherNumberClicked : OnboardingUiEvent

    data object RetryAutomaticNumberClicked : OnboardingUiEvent

    data class PhoneNumberChanged(
        val value: String
    ) : OnboardingUiEvent

    data class NameChanged(
        val value: String
    ) : OnboardingUiEvent

    data object ApproveAndCreateClicked : OnboardingUiEvent
}
