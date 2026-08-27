package com.cbgm.sparrow.feature.attachments.device

import androidx.compose.runtime.Composable
import com.cbgm.sparrow.feature.attachments.domain.model.CurrentLocation

class CurrentLocationLauncher internal constructor(
    private val launchAction: () -> Unit
) {
    fun launch() = launchAction()
}

@Composable
expect fun rememberCurrentLocationLauncher(
    onLocation: (CurrentLocation) -> Unit,
    onError: (String) -> Unit
): CurrentLocationLauncher
