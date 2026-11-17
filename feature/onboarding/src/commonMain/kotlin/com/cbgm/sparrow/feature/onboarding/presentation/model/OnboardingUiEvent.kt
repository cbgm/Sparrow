package com.cbgm.sparrow.feature.onboarding.presentation.model

import com.cbgm.sparrow.feature.transport.background.ConnectionMode

sealed interface OnboardingUiEvent {
    /** The pager settled on [page], by swiping or after the app moved it. */
    data class PageShown(
        val page: OnboardingPage
    ) : OnboardingUiEvent

    data object RequestPermissionsClicked : OnboardingUiEvent

    data object ChooseAnotherNumberClicked : OnboardingUiEvent

    data object RetryAutomaticNumberClicked : OnboardingUiEvent

    data class ConnectionModeChosen(
        val mode: ConnectionMode
    ) : OnboardingUiEvent
}
