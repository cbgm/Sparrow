package com.cbgm.sparrow.feature.onboarding.presentation.model

import com.cbgm.sparrow.feature.transport.background.ConnectionMode

/** [RECEIVING] comes after [PERMISSIONS]: the notification the connection service needs is allowed there. */
enum class OnboardingPage { WELCOME, PRIVACY, PERMISSIONS, RECEIVING, PHONE }

data class OnboardingUiState(
    val page: OnboardingPage = OnboardingPage.WELCOME,
    val permissionRequestId: Int = 0,
    val automaticPhoneRequestId: Int = 0,
    val phoneNumberHintRequestId: Int = 0,
    val permissionsRequested: Boolean = false,
    val phonePermissionGranted: Boolean = false,
    /** How messages reach the device while Sparrow is closed; null until the user chose. */
    val connectionMode: ConnectionMode? = null,
    /** False on iOS: only "while the app is open" exists there, and the page shows a note instead. */
    val connectsWhileClosed: Boolean = true,
    val batteryExemptionRequestId: Int = 0,
    val batteryExemptionDeclined: Boolean = false
)
