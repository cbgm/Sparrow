package com.cbgm.sparrow.startup.presentation.start

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cbgm.sparrow.core.logging.StartupTrace
import com.cbgm.sparrow.feature.onboarding.presentation.OnboardingRoute
import com.cbgm.sparrow.startup.presentation.start.model.StartupConnection
import com.cbgm.sparrow.startup.presentation.start.model.StartupUiEvent
import com.cbgm.sparrow.startup.presentation.start.model.StartupUiState
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun StartupRoute(
    onStartupReady: (StartupConnection) -> Unit,
    onStartupContentReady: () -> Unit,
    startupViewModel: StartupViewModel = koinViewModel()
) {
    val startupUiState by startupViewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(startupUiState) {
        StartupTrace.event(
            "StartupRoute observed state=${when (startupUiState) {
                StartupUiState.Loading -> "Loading"
                is StartupUiState.Ready -> "Ready"
                StartupUiState.IdentityRequired -> "IdentityRequired"
                is StartupUiState.Error -> "Error"
            }}"
        )
        when (val state = startupUiState) {
            is StartupUiState.Ready -> {
                // Keep the native Android splash over navigation to Main;
                // MainRoute releases it independently of network and overview loading.
                startupViewModel.completeStartup()
                onStartupReady(state.connection)
            }
            StartupUiState.IdentityRequired, is StartupUiState.Error -> {
                // These are actual destinations; release the native splash so
                // onboarding or a recoverable error can be displayed.
                onStartupContentReady()
            }
            StartupUiState.Loading -> Unit
        }
    }

    when (val state = startupUiState) {
        StartupUiState.IdentityRequired -> {
            OnboardingRoute(
                onComplete = {
                    startupViewModel.onUiEvent(StartupUiEvent.IdentityCreated)
                }
            )
        }
        is StartupUiState.Error -> {
            StartupErrorScreen(
                message = state.message,
                onRetry = { startupViewModel.onUiEvent(StartupUiEvent.RetryClicked) }
            )
        }
        // No Compose startup loading UI: required initialization continues in
        // the shared StartupViewModel; the platform handles its own launch UI.
        StartupUiState.Loading, is StartupUiState.Ready -> Unit
    }
}
