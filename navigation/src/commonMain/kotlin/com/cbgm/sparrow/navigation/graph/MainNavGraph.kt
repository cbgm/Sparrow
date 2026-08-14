package com.cbgm.sparrow.navigation.graph

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.cbgm.sparrow.core.ui.navigation.AppRoute
import com.cbgm.sparrow.presentation.MainRoute

fun NavGraphBuilder.mainNavGraph() {
    composable<AppRoute.Main> {
        MainRoute()
    }
}
