package com.cbgm.securechat.navigation.graph

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.cbgm.securechat.core.ui.navigation.AppRoute
import com.cbgm.securechat.startup.presentation.StartupRoute

fun NavGraphBuilder.startupNavGraph(onStartupReady: () -> Unit) {
    composable<AppRoute.Startup> {
        StartupRoute(onStartupReady = onStartupReady)
    }
}
