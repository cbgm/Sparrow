package com.cbgm.sparrow.navigation.graph

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.cbgm.sparrow.core.ui.navigation.AppRoute
import com.cbgm.sparrow.feature.contactimport.presentation.importing.ImportIdentityRoute
import com.cbgm.sparrow.feature.contactimport.presentation.scan.ScanIdentityNavigationRoute
import com.cbgm.sparrow.feature.contacts.presentation.blocklist.BlockedContactsRoute
import com.cbgm.sparrow.feature.contacts.presentation.invitations.ContactInvitationRoute
import com.cbgm.sparrow.navigation.slideInFromRight
import com.cbgm.sparrow.navigation.slideOutToRight

fun NavGraphBuilder.contactsNavGraph() {
    composable<AppRoute.ContactInvitations>(
        enterTransition = { slideInFromRight() },
        exitTransition = { slideOutToRight() }
    ) {
        ContactInvitationRoute()
    }

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
