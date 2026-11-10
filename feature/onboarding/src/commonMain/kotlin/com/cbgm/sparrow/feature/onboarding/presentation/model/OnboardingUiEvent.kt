package com.cbgm.sparrow.feature.onboarding.presentation.model

import com.cbgm.sparrow.feature.transport.background.ConnectionMode

sealed interface OnboardingUiEvent {
    data object NextClicked : OnboardingUiEvent

    data object RequestPermissionsClicked : OnboardingUiEvent

    data object ChooseAnotherNumberClicked : OnboardingUiEvent

    data object RetryAutomaticNumberClicked : OnboardingUiEvent

    data class ConnectionModeChosen(
        val mode: ConnectionMode
    ) : OnboardingUiEvent
}
