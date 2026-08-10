package com.cbgm.securechat.navigation.graph

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.cbgm.securechat.core.ui.navigation.AppRoute
import com.cbgm.securechat.feature.identity.presentation.ShareIdentityRoute

fun NavGraphBuilder.identityNavGraph() {
    composable<AppRoute.ShareIdentity> {
        ShareIdentityRoute()
    }
}
