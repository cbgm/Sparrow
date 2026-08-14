package com.cbgm.sparrow.navigation.graph

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.cbgm.sparrow.core.ui.navigation.AppRoute
import com.cbgm.sparrow.startup.presentation.StartupRoute

fun NavGraphBuilder.startupNavGraph(onStartupReady: () -> Unit) {
    composable<AppRoute.Startup> {
        StartupRoute(onStartupReady = onStartupReady)
    }
}
