package com.cbgm.sparrow.startup.presentation.start

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.withFrameNanos
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cbgm.sparrow.core.util.logging.StartupTrace
import com.cbgm.sparrow.feature.applock.presentation.AppLockRoute
import com.cbgm.sparrow.feature.identity.presentation.repair.IdentityRepairRoute
import com.cbgm.sparrow.feature.onboarding.presentation.OnboardingRoute
import com.cbgm.sparrow.startup.presentation.start.model.StartupUiEvent
import com.cbgm.sparrow.startup.presentation.start.model.StartupUiState
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun StartupRoute(
    onStartupReady: () -> Unit,
    onStartupContentReady: () -> Unit,
    startupViewModel: StartupViewModel = koinViewModel()
) {
    val startupUiState by startupViewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(startupUiState) {
        StartupTrace.event(
            "StartupRoute observed state=${startupUiState.traceName()}"
        )
    }

    when (val state = startupUiState) {
        StartupUiState.IdentityRequired -> {
            OnboardingRoute(
                onComplete = {
                    startupViewModel.onUiEvent(StartupUiEvent.IdentityCreated)
                }
            )

            LaunchedEffect(Unit) {
                withFrameNanos { }
                StartupTrace.event("onboarding content ready; releasing native splash")
                onStartupContentReady()
            }
        }

        StartupUiState.Ready -> {
            AppLockRoute(
                onUnlocked = {
                    startupViewModel.completeStartup()
                    onStartupReady()
                }
            )

            LaunchedEffect(Unit) {
                withFrameNanos { }
                StartupTrace.event("startup ready frame; releasing native splash")
                onStartupContentReady()
            }
        }

        StartupUiState.IdentityUnusable -> {
            // After a restore or a reset, startup runs again and decides where to go.
            IdentityRepairRoute(onFinished = { startupViewModel.onUiEvent(StartupUiEvent.RetryClicked) })

            LaunchedEffect(Unit) {
                withFrameNanos { }
                onStartupContentReady()
            }
        }

        is StartupUiState.Error -> {
            StartupErrorScreen(
                message = state.message,
                onRetry = { startupViewModel.onUiEvent(StartupUiEvent.RetryClicked) }
            )

            LaunchedEffect(state) {
                withFrameNanos { }
                StartupTrace.event("startup error content ready; releasing native splash")
                onStartupContentReady()
            }
        }

        StartupUiState.Loading -> Unit
    }
}

private fun StartupUiState.traceName(): String =
    when (this) {
        StartupUiState.Loading -> "Loading"
        StartupUiState.Ready -> "Ready"
        StartupUiState.IdentityRequired -> "IdentityRequired"
        StartupUiState.IdentityUnusable -> "IdentityUnusable"
        is StartupUiState.Error -> "Error"
    }
