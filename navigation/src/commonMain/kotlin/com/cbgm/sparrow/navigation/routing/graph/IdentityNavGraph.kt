package com.cbgm.sparrow.navigation.routing.graph

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.cbgm.sparrow.core.ui.navigation.AppRoute
import com.cbgm.sparrow.feature.identity.presentation.share.ShareIdentityRoute

fun NavGraphBuilder.identityNavGraph() {
    composable<AppRoute.ShareIdentity> {
        ShareIdentityRoute()
    }
}
