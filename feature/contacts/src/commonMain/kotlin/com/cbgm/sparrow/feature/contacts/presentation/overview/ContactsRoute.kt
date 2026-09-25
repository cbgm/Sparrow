package com.cbgm.sparrow.feature.contacts.presentation.overview

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cbgm.sparrow.feature.contacts.device.rememberDeviceContactsPermissionRequest
import com.cbgm.sparrow.feature.contacts.presentation.overview.model.ContactsEffect
import com.cbgm.sparrow.feature.contacts.presentation.overview.model.ContactsScreenMode
import com.cbgm.sparrow.feature.contacts.presentation.overview.model.ContactsUiEvent
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ContactsRoute(
    onEffect: (ContactsEffect) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ContactsViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is ContactsEffect.ShowError -> viewModel.reportError(effect.message)
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
        mode = ContactsScreenMode.Overview(searchQuery = uiState.searchQuery),
        onUiEvent = { event ->
            if (event == ContactsUiEvent.ImportDeviceContacts) {
                requestDeviceContactsPermission()
            } else {
                viewModel.onUiEvent(event)
            }
        },
        modifier = modifier
    )
}
