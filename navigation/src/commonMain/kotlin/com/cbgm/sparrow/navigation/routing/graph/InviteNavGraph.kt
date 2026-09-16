package com.cbgm.sparrow.navigation.routing.graph

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.cbgm.sparrow.core.ui.navigation.AppRoute
import com.cbgm.sparrow.feature.invite.presentation.InvitationRoute
import com.cbgm.sparrow.navigation.routing.slideInFromRight
import com.cbgm.sparrow.navigation.routing.slideOutToRight

fun NavGraphBuilder.inviteNavGraph() {
    composable<AppRoute.Invitations>(
        enterTransition = { slideInFromRight() },
        exitTransition = { slideOutToRight() }
    ) {
        InvitationRoute()
    }
}
