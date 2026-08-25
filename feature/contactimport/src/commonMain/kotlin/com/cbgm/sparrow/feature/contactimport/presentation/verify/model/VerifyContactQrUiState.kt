package com.cbgm.sparrow.feature.contactimport.presentation.verify.model

import com.cbgm.sparrow.feature.contactimport.presentation.scan.model.ScannedIdentityPreview

data class VerifyContactQrUiState(
    val scannedIdentityPreview: ScannedIdentityPreview? = null,
    val isVerifying: Boolean = false,
    val isVerified: Boolean = false,
    val errorMessage: String? = null
)
