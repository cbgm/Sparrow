package com.cbgm.sparrow.feature.identity.presentation.share.model

data class ShareIdentityUiState(
    val isGenerating: Boolean = false,
    val encodedIdentity: String? = null,
    val errorMessage: String? = null
)
