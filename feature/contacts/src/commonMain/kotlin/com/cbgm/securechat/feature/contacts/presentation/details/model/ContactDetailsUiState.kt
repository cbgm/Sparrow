package com.cbgm.securechat.feature.contacts.presentation.details.model

import com.cbgm.securechat.core.crypto.safety.SafetyNumber
import com.cbgm.securechat.feature.contacts.domain.model.Contact

sealed interface ContactDetailsUiState {
    data object Loading : ContactDetailsUiState

    data class Content(
        val contact: Contact,
        val safetyNumber: SafetyNumber?,
        val isSavingVerification: Boolean = false,
        val verificationError: String? = null
    ) : ContactDetailsUiState {
        val canVerify: Boolean
            get() {
                return safetyNumber != null && contact.secureChatIdentity != null
            }
    }

    data object NotFound : ContactDetailsUiState

    data class Error(
        val message: String
    ) : ContactDetailsUiState
}
