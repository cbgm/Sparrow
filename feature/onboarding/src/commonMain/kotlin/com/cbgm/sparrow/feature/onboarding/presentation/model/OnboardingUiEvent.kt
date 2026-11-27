package com.cbgm.sparrow.feature.onboarding.presentation.model

import com.cbgm.sparrow.core.ui.theme.AppThemeMode
import com.cbgm.sparrow.feature.transport.domain.model.ConnectionMode

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

    data object IdentityCreated : OnboardingUiEvent

    data object TourStarted : OnboardingUiEvent

    data object TourNextClicked : OnboardingUiEvent

    data object TourBackSwiped : OnboardingUiEvent

    data object TourSkipped : OnboardingUiEvent

    data class ThemeModeChosen(
        val mode: AppThemeMode
    ) : OnboardingUiEvent
}
