package com.cbgm.securechat.navigation.graph

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.cbgm.securechat.core.ui.navigation.AppRoute
import com.cbgm.securechat.presentation.MainRoute

fun NavGraphBuilder.mainNavGraph() {
    composable<AppRoute.Main> {
        MainRoute()
    }
}
