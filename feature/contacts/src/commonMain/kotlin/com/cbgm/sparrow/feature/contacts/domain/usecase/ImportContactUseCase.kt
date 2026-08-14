package com.cbgm.sparrow.feature.contacts.domain.usecase

import com.cbgm.sparrow.feature.contacts.domain.model.Contact
import com.cbgm.sparrow.feature.contacts.domain.model.ImportContactRequest
import com.cbgm.sparrow.feature.contacts.domain.repository.ContactRepository

/**
 * Imports keys with optional contact details.
 *
 * Both public keys are validated before reaching persistence.
 */
class ImportContactUseCase(
    private val repository: ContactRepository
) {
    suspend operator fun invoke(request: ImportContactRequest): Result<Contact> =
        runCatching {
            require(request.encryptionPublicKey.isNotEmpty()) {
                "Encryption public key must not be empty"
            }

            require(request.signingPublicKey.isNotEmpty()) {
                "Signing public key must not be empty"
            }

            repository
                .importContact(
                    request =
                        request.copy(
                            displayName =
                                request.displayName
                                    ?.trim()
                                    ?.takeIf { it.isNotEmpty() },
                            phoneNumber =
                                request.phoneNumber
                                    ?.trim()
                                    ?.takeIf { it.isNotEmpty() }
                        )
                ).getOrThrow()
        }
}
