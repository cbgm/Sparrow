package com.cbgm.securechat.navigation.graph

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
        val destination = backStackEntry.toRoute<AppRoute.ImportContact>()
        val scannedIdentityFromScanner by
            backStackEntry.savedStateHandle
                .getStateFlow<String?>(SCANNED_IDENTITY_KEY, null)
                .collectAsStateWithLifecycle()
        var destinationScannedIdentity by remember(destination.scannedIdentity) {
            mutableStateOf(destination.scannedIdentity)
        }

        ImportIdentityRoute(
            contactId = destination.contactId,
            scannedIdentity = scannedIdentityFromScanner ?: destinationScannedIdentity,
            onScannedIdentityConsumed = {
                backStackEntry.savedStateHandle.remove<String>(SCANNED_IDENTITY_KEY)
                destinationScannedIdentity = null
            }
        )
    }

    composable<AppRoute.ScanIdentity> {
        ScanIdentityNavigationRoute()
    }
}

private const val SCANNED_IDENTITY_KEY = "scannedIdentity"
