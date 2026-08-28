package com.cbgm.sparrow.navigation.routing.graph

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.cbgm.sparrow.core.ui.navigation.AppRoute
import com.cbgm.sparrow.feature.contactimport.presentation.importing.ImportIdentityRoute
import com.cbgm.sparrow.feature.contactimport.presentation.scan.ScanIdentityNavigationRoute
import com.cbgm.sparrow.feature.contacts.presentation.blocklist.BlockedContactsRoute
import com.cbgm.sparrow.feature.contacts.presentation.invitations.ContactInvitationRoute
import com.cbgm.sparrow.navigation.routing.slideInFromRight
import com.cbgm.sparrow.navigation.routing.slideOutToRight

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

    composable<AppRoute.ImportContact> {
        ImportIdentityRoute()
    }

    composable<AppRoute.ScanIdentity> {
        ScanIdentityNavigationRoute()
    }
}
