package com.cbgm.sparrow.feature.contactimport.domain.usecase

import com.cbgm.sparrow.feature.contacts.domain.model.Contact
import com.cbgm.sparrow.feature.contacts.domain.model.IdentityImportTrust
import com.cbgm.sparrow.feature.contacts.domain.model.ImportContactRequest
import com.cbgm.sparrow.feature.contacts.domain.usecase.ImportContactUseCase
import com.cbgm.sparrow.feature.identity.domain.repository.IdentityShareRepository

/**
 * Decodes and imports a shared Sparrow identity.
 *
 * A phone number is mandatory because it is the stable contact,
 * conversation, and routing anchor. The contact repository then
 * merges by normalized phone number before considering public keys.
 */
class ImportSharedIdentityUseCase(
    private val identityShareRepository: IdentityShareRepository,
    private val importContact: ImportContactUseCase
) {
    suspend operator fun invoke(
        encodedIdentity: String,
        contactId: String? = null,
        identityImportTrust: IdentityImportTrust = IdentityImportTrust.UNVERIFIED
    ): Result<Contact> =
        runCatching {
            val sharedIdentity = identityShareRepository.decode(encodedIdentity).getOrThrow()

            val phoneNumber =
                sharedIdentity
                    .contactDetails
                    .phoneNumber
                    .trim()
                    .takeIf { it.isNotEmpty() }
                    ?: error(
                        "Shared identity does not contain a phone number"
                    )

            importContact(
                request =
                    ImportContactRequest(
                        contactId = contactId,
                        encryptionPublicKey = sharedIdentity.encryptionPublicKey.copyOf(),
                        signingPublicKey = sharedIdentity.signingPublicKey.copyOf(),
                        displayName = sharedIdentity.contactDetails.displayName,
                        phoneNumber = phoneNumber,
                        identityImportTrust = identityImportTrust
                    )
            ).getOrThrow()
        }
}
