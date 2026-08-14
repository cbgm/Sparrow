package com.cbgm.securechat.feature.contactimport.presentation.scan

import com.cbgm.securechat.core.ui.navigation.AppRoute
import com.cbgm.securechat.core.ui.presentation.BaseViewModel
import com.cbgm.securechat.feature.contactimport.presentation.scan.model.ScanIdentityUiEvent

class ScanIdentityNavigationViewModel(
    private val route: AppRoute.ScanIdentity
) : BaseViewModel() {
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
                    contactId = route.contactId
                ),
            popUpTo =
                AppRoute.ImportContact(
                    scannedIdentity = route.previousScannedIdentity,
                    contactId = route.contactId
                ),
            inclusive = true
        )
    }
}
