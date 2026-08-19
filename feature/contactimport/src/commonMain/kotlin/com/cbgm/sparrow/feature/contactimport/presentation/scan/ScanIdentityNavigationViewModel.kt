package com.cbgm.sparrow.feature.contactimport.presentation.scan

import androidx.lifecycle.SavedStateHandle
import com.cbgm.sparrow.core.ui.navigation.AppRoute
import com.cbgm.sparrow.core.ui.presentation.BaseViewModel
import com.cbgm.sparrow.feature.contactimport.presentation.scan.model.ScanIdentityUiEvent

class ScanIdentityNavigationViewModel(
    savedStateHandle: SavedStateHandle
) : BaseViewModel() {
    private val contactId = savedStateHandle.get<String>(AppRoute.ScanIdentity::contactId.name)
    private val previousScannedIdentity =
        savedStateHandle.get<String>(AppRoute.ScanIdentity::previousScannedIdentity.name)

    fun onUiEvent(event: ScanIdentityUiEvent) {
        when (event) {
            is ScanIdentityUiEvent.QrCodeScanned -> onQrCodeScanned(event.encodedIdentity)
            ScanIdentityUiEvent.BackClicked -> navigator.popBackStack()
        }
    }

    private fun onQrCodeScanned(encodedIdentity: String) {
        navigator.navigateTo(
            route =
                AppRoute.ImportContact(
                    scannedIdentity = encodedIdentity,
                    contactId = contactId
                ),
            popUpTo =
                AppRoute.ImportContact(
                    scannedIdentity = previousScannedIdentity,
                    contactId = contactId
                ),
            inclusive = true
        )
    }
}
