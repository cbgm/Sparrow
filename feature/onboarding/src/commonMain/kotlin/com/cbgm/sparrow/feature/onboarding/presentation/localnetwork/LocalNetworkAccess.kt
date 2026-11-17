package com.cbgm.sparrow.feature.onboarding.presentation.localnetwork

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cbgm.sparrow.feature.onboarding.device.LocalNetworkPermissionRequester
import com.cbgm.sparrow.resources.Res
import com.cbgm.sparrow.resources.feature_local_network_denied
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/** Asks for the local network permission when a configured server is on the local network. Placed once in the app shell. */
@Composable
fun LocalNetworkAccess(viewModel: LocalNetworkAccessViewModel = koinViewModel()) {
    val needed by viewModel.needed.collectAsStateWithLifecycle()
    LocalNetworkPermissionRequester(needed = needed, deniedMessage = stringResource(Res.string.feature_local_network_denied))
}
