package com.cbgm.securechat.feature.onboarding.device

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect

@Composable
actual fun OnboardingPermissionRequester(
    requestId: Int,
    onResult: (PermissionRequestResult) -> Unit
) {
    LaunchedEffect(requestId) {
        if (requestId > 0) { onResult(PermissionRequestResult(
                    contactsGranted = false,
                    cameraGranted = false,
                    notificationsGranted = false,
                    phoneNumberGranted = false
                )
            )
        }
    }
}

@Composable
actual fun AutomaticPhoneNumberReader(
    requestId: Int,
    enabled: Boolean,
    onResult: (AutomaticPhoneNumberResult) -> Unit
) {
    LaunchedEffect(requestId, enabled) {
        if (enabled && requestId > 0) onResult(AutomaticPhoneNumberResult.Unavailable)
    }
}
