package com.cbgm.sparrow.feature.onboarding.device

import androidx.compose.runtime.Composable

/**
 * Asks to leave Sparrow out of battery optimization, so the connection kept while the app is closed does not drop in
 * deep sleep; [onResult] tells whether Sparrow is left out now.
 */
@Composable
expect fun BatteryExemptionRequester(
    requestId: Int,
    onResult: (exempted: Boolean) -> Unit
)
