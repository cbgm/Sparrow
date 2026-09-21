package com.cbgm.sparrow.feature.identity.presentation.recovery

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun IdentityRecoveryRoute(
    startFreshInvitation: suspend (String) -> Result<Unit>,
    scanner: @Composable ((String) -> Unit, () -> Unit) -> Unit,
    viewModel: IdentityRecoveryViewModel = koinViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var scanning by remember { mutableStateOf(false) }
    if (scanning) {
        scanner(
            { encoded ->
                if (scanning) {
                    scanning = false
                    viewModel.confirmScannedIdentity(encoded)
                }
            },
            { scanning = false }
        )
        return
    }
    IdentityRecoveryScreen(
        state = state,
        onClose = viewModel::close,
        onConfirm = viewModel::confirm,
        onScan = { scanning = true },
        onApprove = { viewModel.approve(startFreshInvitation) },
        onDismiss = viewModel::dismiss,
        onRetry = { viewModel.retryReconnection(startFreshInvitation) }
    )
}
