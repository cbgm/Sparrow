package com.cbgm.sparrow.navigation.routing.graph

import androidx.compose.animation.ExitTransition
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.cbgm.sparrow.core.ui.navigation.AppRoute
import com.cbgm.sparrow.startup.presentation.start.StartupRoute

fun NavGraphBuilder.startupNavGraph(
    onStartupReady: () -> Unit,
    onStartupContentReady: () -> Unit
) {
    composable<AppRoute.Startup>(exitTransition = { ExitTransition.None }) {
        StartupRoute(
            onStartupReady = onStartupReady,
            onStartupContentReady = onStartupContentReady
        )
    }
}
