package com.cbgm.sparrow.startup.presentation.start

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cbgm.sparrow.feature.identity.presentation.setup.model.IdentityUiState
import com.cbgm.sparrow.feature.onboarding.presentation.OnboardingRoute
import com.cbgm.sparrow.startup.presentation.start.model.StartupConnection
import com.cbgm.sparrow.startup.presentation.start.model.StartupUiEvent
import com.cbgm.sparrow.startup.presentation.start.model.StartupUiState
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun StartupRoute(
    onStartupReady: (StartupConnection) -> Unit,
    startupViewModel: StartupViewModel = koinViewModel()
) {
    val startupUiState by startupViewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(startupUiState) {
        val ready = startupUiState as? StartupUiState.Ready ?: return@LaunchedEffect
        startupViewModel.completeStartup()
        onStartupReady(ready.connection)
    }

    when (val state = startupUiState) {
        StartupUiState.IdentityRequired -> {
            OnboardingRoute(
                onComplete = {
                    startupViewModel.onUiEvent(StartupUiEvent.IdentityCreated)
                }
            )
        }

        else -> {
            StartupScreen(
                uiState = state,
                identityUiState = IdentityUiState.Loading,
                onUiEvent = startupViewModel::onUiEvent
            )
        }
    }
}
