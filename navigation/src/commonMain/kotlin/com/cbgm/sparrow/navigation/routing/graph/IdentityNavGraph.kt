package com.cbgm.sparrow.navigation.routing.graph

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.cbgm.sparrow.core.ui.navigation.AppRoute
import com.cbgm.sparrow.feature.identity.presentation.setup.IdentityRoute
import com.cbgm.sparrow.feature.identity.presentation.setup.MeDetailPage
import com.cbgm.sparrow.feature.identity.presentation.share.ShareIdentityRoute

fun NavGraphBuilder.identityNavGraph() {
    composable<AppRoute.IdentityKeys> {
        IdentityRoute(scrollState = rememberScrollState(), innerPadding = PaddingValues(0.dp), page = MeDetailPage.Keys)
    }
    composable<AppRoute.IdentityBackup> {
        IdentityRoute(scrollState = rememberScrollState(), innerPadding = PaddingValues(0.dp), page = MeDetailPage.Backup)
    }
    composable<AppRoute.ShareIdentity> {
        ShareIdentityRoute()
    }
}
