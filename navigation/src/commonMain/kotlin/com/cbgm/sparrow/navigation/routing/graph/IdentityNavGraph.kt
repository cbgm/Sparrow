package com.cbgm.sparrow.navigation.routing.graph

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.cbgm.sparrow.core.ui.navigation.AppRoute
import com.cbgm.sparrow.feature.contactimport.presentation.scan.ScanIdentityRoute
import com.cbgm.sparrow.feature.contactimport.presentation.scan.model.ScanIdentityUiEvent
import com.cbgm.sparrow.feature.conversationorchestration.domain.usecase.StartRecoveryInvitationUseCase
import com.cbgm.sparrow.feature.identity.presentation.recovery.IdentityRecoveryRoute
import com.cbgm.sparrow.feature.identity.presentation.share.ShareIdentityRoute
import org.koin.compose.koinInject

fun NavGraphBuilder.identityNavGraph() {
    composable<AppRoute.ShareIdentity> {
        ShareIdentityRoute()
    }
    composable<AppRoute.IdentityRecovery> {
        val startInvitation: StartRecoveryInvitationUseCase = koinInject()
        IdentityRecoveryRoute(
            startFreshInvitation = { peerId -> startInvitation(peerId) },
            scanner = { onScanned, onCancel ->
                ScanIdentityRoute(onUiEvent = { event ->
                    when (event) {
                        is ScanIdentityUiEvent.QrCodeScanned -> onScanned(event.encodedIdentity)
                        ScanIdentityUiEvent.BackClicked -> onCancel()
                    }
                })
            }
        )
    }
}
