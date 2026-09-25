package com.cbgm.sparrow.feature.contacts.presentation.details.model

sealed interface ContactDetailsUiState {
    data object Loading : ContactDetailsUiState

    data class Content(
        val contact: ContactDetailsContactUi,
        val safetyNumber: String?,
        val isSavingVerification: Boolean = false,
        val verificationError: String? = null
    ) : ContactDetailsUiState {
        val canVerify: Boolean
            get() {
                return safetyNumber != null && contact.sparrowIdentity != null
            }
    }

    data object NotFound : ContactDetailsUiState

    data class Error(
        val message: String
    ) : ContactDetailsUiState
}
