package com.cbgm.securechat.feature.settings.presentation

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cbgm.securechat.feature.settings.presentation.model.SettingsEffect
import com.cbgm.securechat.feature.settings.presentation.model.SettingsEvent
import com.cbgm.securechat.feature.settings.presentation.screen.SettingsScreen
import com.cbgm.securechat.feature.settings.presentation.screen.SettingsViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SettingsRoute(
    modifier: Modifier = Modifier,
    scrollState: ScrollState,
    innerPadding: PaddingValues,
    onNavigateToPrivacyPolicy: () -> Unit,
    onNavigateToDataDisclaimer: () -> Unit,
    onNavigateToLicenses: () -> Unit,
    onNavigateToDeveloperMenu: () -> Unit,
    onNavigateToBlockedContacts: () -> Unit,
    onNavigateToControlPlanes: () -> Unit,
    viewModel: SettingsViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is SettingsEffect.ShowSnackbar -> snackbarHostState.showSnackbar(effect.message)
            }
        }
    }

    SettingsScreen(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onOpenPrivacyPolicy = onNavigateToPrivacyPolicy,
        onOpenDataDisclaimer = onNavigateToDataDisclaimer,
        onOpenLicenses = onNavigateToLicenses,
        onOpenDeveloperMenu = onNavigateToDeveloperMenu,
        onOpenBlockedContacts = onNavigateToBlockedContacts,
        onOpenControlPlanes = onNavigateToControlPlanes,
        onOpenLanguagePicker = { viewModel.onEvent(SettingsEvent.LanguagePickerOpened) },
        onDismissLanguagePicker = { viewModel.onEvent(SettingsEvent.LanguagePickerDismissed) },
        onLanguageSelected = { viewModel.onEvent(SettingsEvent.LanguageSelected(it)) },
        onDirectIdentitySetupModeChanged = {
            viewModel.onEvent(SettingsEvent.DirectIdentitySetupModeChanged(it))
        },
        onBlockUnknownContactInvitesChanged = {
            viewModel.onEvent(SettingsEvent.BlockUnknownContactInvitesChanged(it))
        },
        onVersionRowTapped = { viewModel.onEvent(SettingsEvent.VersionRowTapped) },
        scrollState = scrollState,
        innerPadding = innerPadding,
        modifier = modifier
    )
}
