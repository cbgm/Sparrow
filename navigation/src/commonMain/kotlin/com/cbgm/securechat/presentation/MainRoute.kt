package com.cbgm.securechat.presentation

import androidx.compose.runtime.Composable
import com.cbgm.securechat.presentation.screen.MainScreen

@Composable
fun MainRoute(
    onImportContact: () -> Unit,
    onNavigateToPrivacyPolicy: () -> Unit,
    onNavigateToDataDisclaimer: () -> Unit,
    onNavigateToLicenses: () -> Unit,
    onNavigateToDeveloperMenu: () -> Unit,
    onNavigateToBlockedContacts: () -> Unit,
    onNavigateToControlPlanes: () -> Unit,
    onOpenChat: (String, String, String, Boolean) -> Unit,
    onShareIdentity: () -> Unit
) {
    MainScreen(
        onImportContact = onImportContact,
        onNavigateToPrivacyPolicy = onNavigateToPrivacyPolicy,
        onNavigateToDataDisclaimer = onNavigateToDataDisclaimer,
        onNavigateToLicenses = onNavigateToLicenses,
        onNavigateToDeveloperMenu = onNavigateToDeveloperMenu,
        onNavigateToBlockedContacts = onNavigateToBlockedContacts,
        onNavigateToControlPlanes = onNavigateToControlPlanes,
        onOpenChat = onOpenChat,
        onShareIdentity = onShareIdentity
    )
}
