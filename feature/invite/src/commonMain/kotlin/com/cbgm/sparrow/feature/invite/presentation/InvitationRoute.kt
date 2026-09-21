package com.cbgm.sparrow.feature.invite.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cbgm.sparrow.feature.invite.presentation.model.MailboxReviewRequestUi
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun InvitationRoute(
    recoveryRequests: List<MailboxReviewRequestUi> = emptyList(),
    recoveryRequestsLoaded: Boolean = true,
    onReviewRecovery: (String, String) -> Unit = { _, _ -> },
    viewModel: InvitationViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(recoveryRequests.size, recoveryRequestsLoaded) {
        viewModel.setRecoveryRequestCount(if (recoveryRequestsLoaded) recoveryRequests.size else -1)
    }

    InvitationsScreen(
        uiState = uiState,
        recoveryRequests = recoveryRequests,
        onReviewRecovery = onReviewRecovery,
        onUiEvent = viewModel::onUiEvent
    )
}
