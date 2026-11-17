package com.cbgm.sparrow.feature.onboarding.device

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect

/** iOS keeps no connection in the background, so there is nothing to leave out. */
@Composable
actual fun BatteryExemptionRequester(
    requestId: Int,
    onResult: (exempted: Boolean) -> Unit
) {
    LaunchedEffect(requestId) {
        if (requestId > 0) onResult(false)
    }
}
