package com.cbgm.sparrow.feature.onboarding.device

import androidx.compose.runtime.Composable

/**
 * Asks for the local network permission while [needed] (a configured server is on the local network) and the system
 * requires it (Android 17 and later) but it is not granted, at most once per app start; when the user refuses,
 * [deniedMessage] tells that local servers cannot be reached.
 */
@Composable
expect fun LocalNetworkPermissionRequester(
    needed: Boolean,
    deniedMessage: String
)
