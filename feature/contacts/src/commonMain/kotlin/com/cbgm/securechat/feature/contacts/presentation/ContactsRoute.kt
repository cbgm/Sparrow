package com.cbgm.securechat.feature.contacts.presentation

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cbgm.securechat.feature.contacts.presentation.model.ContactsEffect
import com.cbgm.securechat.feature.contacts.presentation.model.ContactsScreenMode
import com.cbgm.securechat.feature.contacts.presentation.model.ContactsUiEvent
import com.cbgm.securechat.feature.contacts.presentation.platform.rememberDeviceContactsPermissionRequest
import com.cbgm.securechat.feature.contacts.presentation.screen.ContactsScreen
import com.cbgm.securechat.feature.contacts.presentation.screen.ContactsViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ContactsRoute(
    onEffect: (ContactsEffect) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ContactsViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is ContactsEffect.ShowError -> snackbarHostState.showSnackbar(effect.message)
                else -> onEffect(effect)
            }
        }
    }

    val requestDeviceContactsPermission =
        rememberDeviceContactsPermissionRequest(
            onPermissionGranted = {
                viewModel.onUiEvent(ContactsUiEvent.ImportDeviceContacts)
            },
            onPermissionDenied = {
                viewModel.onUiEvent(ContactsUiEvent.DeviceContactsPermissionDenied)
            }
        )

    LaunchedEffect(Unit) {
        requestDeviceContactsPermission()
    }

    ContactsScreen(
        uiState = uiState,
        mode = ContactsScreenMode.Overview(searchQuery = searchQuery),
        onUiEvent = { event ->
            if (event == ContactsUiEvent.ImportDeviceContacts) {
                requestDeviceContactsPermission()
            } else {
                viewModel.onUiEvent(event)
            }
        },
        modifier = modifier,
        snackbarHostState = snackbarHostState
    )
}
