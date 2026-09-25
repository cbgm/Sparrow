package com.cbgm.sparrow.navigation.routing.graph

import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.cbgm.sparrow.core.ui.navigation.AppRoute
import com.cbgm.sparrow.feature.invite.presentation.InvitationRoute
import com.cbgm.sparrow.navigation.presentation.inbox.RecoveryInboxViewModel
import com.cbgm.sparrow.navigation.routing.slideInFromRight
import com.cbgm.sparrow.navigation.routing.slideOutToRight
import org.koin.compose.viewmodel.koinViewModel

fun NavGraphBuilder.inviteNavGraph() {
    composable<AppRoute.Invitations>(
        enterTransition = { slideInFromRight() },
        exitTransition = { slideOutToRight() }
    ) {
        val recoveryViewModel: RecoveryInboxViewModel = koinViewModel()
        val requests by recoveryViewModel.requests.collectAsStateWithLifecycle()
        val loaded by recoveryViewModel.loaded.collectAsStateWithLifecycle()
        val processing by recoveryViewModel.processing.collectAsStateWithLifecycle()
        val error by recoveryViewModel.error.collectAsStateWithLifecycle()
        InvitationRoute(
            recoveryRequests = requests,
            recoveryRequestsLoaded = loaded,
            processingRecoveryId = processing,
            recoveryError = error,
            onApproveRecovery = recoveryViewModel::approve,
            onDeclineRecovery = recoveryViewModel::decline,
            onBlockRecovery = recoveryViewModel::block
        )
    }
}
