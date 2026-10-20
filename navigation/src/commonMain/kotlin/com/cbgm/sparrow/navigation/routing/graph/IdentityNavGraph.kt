package com.cbgm.sparrow.navigation.routing.graph

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.cbgm.sparrow.core.ui.navigation.AppRoute
import com.cbgm.sparrow.feature.identity.presentation.backup.IdentityBackupRoute
import com.cbgm.sparrow.feature.identity.presentation.details.IdentityDetailsRoute
import com.cbgm.sparrow.feature.identity.presentation.share.ShareIdentityRoute
import com.cbgm.sparrow.navigation.routing.slideInFromRight
import com.cbgm.sparrow.navigation.routing.slideOutToRight

fun NavGraphBuilder.identityNavGraph() {
    composable<AppRoute.IdentityKeys>(
        enterTransition = { slideInFromRight() },
        exitTransition = { slideOutToRight() }
    ) {
        IdentityDetailsRoute()
    }
    composable<AppRoute.IdentityBackup>(
        enterTransition = { slideInFromRight() },
        exitTransition = { slideOutToRight() }
    ) {
        IdentityBackupRoute()
    }
    composable<AppRoute.ShareIdentity>(
        enterTransition = { slideInFromRight() },
        exitTransition = { slideOutToRight() }
    ) {
        ShareIdentityRoute()
    }
}
