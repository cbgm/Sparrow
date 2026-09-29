package com.cbgm.sparrow.startup.presentation.start

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cbgm.sparrow.core.logging.StartupTrace
import com.cbgm.sparrow.feature.applock.presentation.AppLockRoute
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
            "StartupRoute observed state=${startupUiState.traceName()}"
        )

        when (startupUiState) {
            StartupUiState.IdentityRequired,
            is StartupUiState.Error -> {
                onStartupContentReady()
            }

            StartupUiState.Loading,
            is StartupUiState.Ready -> Unit
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

        is StartupUiState.Ready -> {
            AppLockRoute(
                onUnlocked = {
                    startupViewModel.completeStartup()
                    onStartupReady(state.connection)
                },
                onLockedContentReady = onStartupContentReady
            )
        }

        is StartupUiState.Error -> {
            StartupErrorScreen(
                message = state.message,
                onRetry = { startupViewModel.onUiEvent(StartupUiEvent.RetryClicked) }
            )
        }

        StartupUiState.Loading -> Unit
    }
}

private fun StartupUiState.traceName(): String =
    when (this) {
        StartupUiState.Loading -> "Loading"
        is StartupUiState.Ready -> "Ready"
        StartupUiState.IdentityRequired -> "IdentityRequired"
        is StartupUiState.Error -> "Error"
    }
