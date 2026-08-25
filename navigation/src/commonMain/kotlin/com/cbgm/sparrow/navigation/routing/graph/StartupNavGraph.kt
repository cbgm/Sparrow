package com.cbgm.sparrow.navigation.routing.graph

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.cbgm.sparrow.core.ui.navigation.AppRoute
import com.cbgm.sparrow.startup.presentation.start.StartupRoute
import com.cbgm.sparrow.startup.presentation.start.model.StartupConnection

fun NavGraphBuilder.startupNavGraph(
    onStartupReady: (StartupConnection) -> Unit
) {
    composable<AppRoute.Startup> {
        StartupRoute(onStartupReady = onStartupReady)
    }
}
