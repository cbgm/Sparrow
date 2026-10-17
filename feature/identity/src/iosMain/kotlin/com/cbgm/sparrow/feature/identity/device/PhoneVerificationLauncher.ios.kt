package com.cbgm.sparrow.feature.identity.device

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect

@Composable
actual fun PhoneVerificationLauncher(
    request: PhoneVerificationRequest?,
    onResult: (PhoneVerificationResult) -> Unit
) {
    LaunchedEffect(request?.id) {
        if (request != null) onResult(PhoneVerificationResult.Unsupported(request.id))
    }
}
