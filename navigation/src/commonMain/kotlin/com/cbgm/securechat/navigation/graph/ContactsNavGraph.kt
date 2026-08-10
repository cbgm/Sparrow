package com.cbgm.securechat.navigation.graph

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.cbgm.securechat.core.ui.navigation.AppRoute
import com.cbgm.securechat.feature.contactimport.presentation.ImportIdentityRoute
import com.cbgm.securechat.feature.contactimport.presentation.ScanIdentityNavigationRoute
import com.cbgm.securechat.feature.contacts.presentation.BlockedContactsRoute
import com.cbgm.securechat.navigation.slideInFromRight
import com.cbgm.securechat.navigation.slideOutToRight

fun NavGraphBuilder.contactsNavGraph() {
    composable<AppRoute.BlockedContacts>(
        enterTransition = { slideInFromRight() },
        exitTransition = { slideOutToRight() }
    ) {
        BlockedContactsRoute()
    }

    composable<AppRoute.ImportContact> { backStackEntry ->
        ImportIdentityRoute(
            route = backStackEntry.toRoute<AppRoute.ImportContact>()
        )
    }

    composable<AppRoute.ScanIdentity> { backStackEntry ->
        ScanIdentityNavigationRoute(
            route = backStackEntry.toRoute<AppRoute.ScanIdentity>()
        )
    }
}
