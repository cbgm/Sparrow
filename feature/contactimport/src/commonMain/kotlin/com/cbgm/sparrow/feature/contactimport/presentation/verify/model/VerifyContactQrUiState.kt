package com.cbgm.sparrow.feature.contactimport.presentation.verify.model

data class VerifyContactQrUiState(
    val isVerifying: Boolean = false,
    val isVerified: Boolean = false,
    val errorMessage: String? = null
)
