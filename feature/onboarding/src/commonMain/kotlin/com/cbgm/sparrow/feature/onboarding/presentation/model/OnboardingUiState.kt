package com.cbgm.sparrow.feature.onboarding.presentation.model

import com.cbgm.sparrow.core.ui.theme.AppThemeMode
import com.cbgm.sparrow.feature.transport.background.ConnectionMode

/** [RECEIVING] comes after [PERMISSIONS]: the notification the connection service needs is allowed there. */
enum class OnboardingPage { WELCOME, ONION, PERMISSIONS, RECEIVING, THEME, PHONE }

data class OnboardingUiState(
    val page: OnboardingPage = OnboardingPage.WELCOME,
    val permissionRequestId: Int = 0,
    val automaticPhoneRequestId: Int = 0,
    val phoneNumberHintRequestId: Int = 0,
    val permissionsRequested: Boolean = false,
    val phonePermissionGranted: Boolean = false,
    /** How messages reach the device while Sparrow is closed; preselected, saved once chosen or swiped past. */
    val connectionMode: ConnectionMode = ConnectionMode.ALWAYS_CONNECTED,
    val connectionModeSaved: Boolean = false,
    /** False on iOS: only "while the app is open" exists there, and the page shows a note instead. */
    val connectsWhileClosed: Boolean = true,
    val batteryExemptionRequestId: Int = 0,
    val batteryExemptionDeclined: Boolean = false,
    val themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    /** The key-feature tour after the identity is created; null before it starts. */
    val tourStep: TourStep? = null,
    val tourFinished: Boolean = false
)

/** Only what no screen explains by itself. */
enum class TourStep { RING, SHIELD, SEARCH, MAILBOX, DOTS, HOLD, SWIPE }
