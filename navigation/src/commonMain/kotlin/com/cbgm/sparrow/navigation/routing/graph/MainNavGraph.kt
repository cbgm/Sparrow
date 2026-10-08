package com.cbgm.sparrow.navigation.routing.graph

import androidx.compose.animation.EnterTransition
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.withFrameNanos
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.cbgm.sparrow.core.ui.navigation.AppRoute
import com.cbgm.sparrow.navigation.presentation.main.MainRoute

fun NavGraphBuilder.mainNavGraph(
    onContentReady: () -> Unit = {}
) {
    composable<AppRoute.Main>(enterTransition = { EnterTransition.None }) {
        MainRoute()

        LaunchedEffect(Unit) {
            withFrameNanos { }
            onContentReady()
        }
    }
}
