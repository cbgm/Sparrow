package com.cbgm.sparrow.feature.contacts.presentation.details.mapper

import com.cbgm.sparrow.core.crypto.safety.SafetyNumber
import com.cbgm.sparrow.feature.contacts.domain.model.Contact
import com.cbgm.sparrow.feature.contacts.presentation.details.model.ContactDetailsUiState

internal fun Contact.toUiState(
    safetyNumber: SafetyNumber?,
    profilePictureBytes: ByteArray?
): ContactDetailsUiState.Content =
    ContactDetailsUiState.Content(
        contact = this,
        safetyNumber = safetyNumber,
        profilePictureBytes = profilePictureBytes
    )

internal fun ContactDetailsUiState.withVerificationState(
    isSaving: Boolean,
    errorMessage: String?
): ContactDetailsUiState =
    if (this is ContactDetailsUiState.Content) {
        copy(
            isSavingVerification = isSaving,
            verificationError = errorMessage
        )
    } else {
        this
    }
