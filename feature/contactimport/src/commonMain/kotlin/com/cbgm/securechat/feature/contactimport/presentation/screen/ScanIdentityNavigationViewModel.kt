package com.cbgm.securechat.feature.contactimport.presentation.screen

import com.cbgm.securechat.core.ui.navigation.AppNavigationResult
import com.cbgm.securechat.core.ui.presentation.BaseViewModel
import com.cbgm.securechat.feature.contactimport.presentation.model.ScanIdentityUiEvent

class ScanIdentityNavigationViewModel : BaseViewModel() {
    fun onUiEvent(event: ScanIdentityUiEvent) {
        when (event) {
            is ScanIdentityUiEvent.QrCodeScanned -> onQrCodeScanned(event.encodedIdentity)
            ScanIdentityUiEvent.BackClicked -> navigateBack()
        }
    }

    private fun onQrCodeScanned(encodedIdentity: String) {
        navigator.popBackStack(
            result =
                AppNavigationResult.StringValue(
                    key = SCANNED_IDENTITY_KEY,
                    value = encodedIdentity
                )
        )
    }

    private fun navigateBack() {
        navigator.popBackStack()
    }

    private companion object {
        const val SCANNED_IDENTITY_KEY = "scannedIdentity"
    }
}
