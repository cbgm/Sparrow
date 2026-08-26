package com.cbgm.sparrow.feature.onboarding.device

import androidx.compose.runtime.Composable

data class PermissionRequestResult(
    val contactsGranted: Boolean,
    val cameraGranted: Boolean,
    val audioGranted: Boolean,
    val notificationsGranted: Boolean,
    val phoneNumberGranted: Boolean,
    val storageGranted: Boolean
)

sealed interface AutomaticPhoneNumberResult {
    data class Found(
        val phoneNumber: String
    ) : AutomaticPhoneNumberResult

    data object Unavailable : AutomaticPhoneNumberResult

    data class Failed(
        val message: String
    ) : AutomaticPhoneNumberResult
}

@Composable
expect fun OnboardingPermissionRequester(
    requestId: Int,
    onResult: (PermissionRequestResult) -> Unit
)

@Composable
expect fun AutomaticPhoneNumberReader(
    requestId: Int,
    enabled: Boolean,
    onResult: (AutomaticPhoneNumberResult) -> Unit
)
