package com.cbgm.sparrow.feature.onboarding.device

import androidx.compose.runtime.Composable

/** iOS asks for local network access by itself on the first connection. */
@Composable
actual fun LocalNetworkPermissionRequester(
    needed: Boolean,
    deniedMessage: String
) = Unit
